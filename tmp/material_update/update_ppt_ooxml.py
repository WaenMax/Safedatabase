from __future__ import annotations

import copy
import shutil
from pathlib import Path
from xml.etree import ElementTree as ET
from zipfile import ZIP_DEFLATED, ZipFile


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "outputs" / "数据分类分级保护系统-答辩PPT.pptx"
OUT = ROOT / "outputs" / "数据分类分级保护系统-答辩PPT-优化版.pptx"
SHOT_DIR = ROOT / "outputs" / "project_screenshots"

NS = {
    "p": "http://schemas.openxmlformats.org/presentationml/2006/main",
    "a": "http://schemas.openxmlformats.org/drawingml/2006/main",
    "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
    "ct": "http://schemas.openxmlformats.org/package/2006/content-types",
    "rel": "http://schemas.openxmlformats.org/package/2006/relationships",
}

for prefix, uri in NS.items():
    ET.register_namespace("" if prefix in {"ct", "rel"} else prefix, uri)


EMU_PER_PT = 12700


def emu(value: float) -> int:
    return int(round(value * EMU_PER_PT))


def slide_xml(title: str, subtitle: str, pictures: list[dict], notes: list[str] | None = None) -> bytes:
    notes = notes or []
    shape_id = 2

    def text_shape(text: str, x: float, y: float, w: float, h: float, size: int, color: str, bold: bool = False, align: str = "l") -> str:
        nonlocal shape_id
        sid = shape_id
        shape_id += 1
        b_attr = ' b="1"' if bold else ""
        return f"""
        <p:sp>
          <p:nvSpPr><p:cNvPr id="{sid}" name="TextBox {sid}"/><p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr>
          <p:spPr>
            <a:xfrm><a:off x="{emu(x)}" y="{emu(y)}"/><a:ext cx="{emu(w)}" cy="{emu(h)}"/></a:xfrm>
            <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
            <a:noFill/><a:ln><a:noFill/></a:ln>
          </p:spPr>
          <p:txBody>
            <a:bodyPr wrap="square" anchor="t"><a:spAutoFit/></a:bodyPr><a:lstStyle/>
            <a:p><a:pPr algn="{align}"/>
              <a:r><a:rPr lang="zh-CN" sz="{size * 100}"{b_attr}>
                <a:solidFill><a:srgbClr val="{color}"/></a:solidFill>
                <a:latin typeface="Microsoft YaHei"/><a:ea typeface="Microsoft YaHei"/>
              </a:rPr><a:t>{escape_xml(text)}</a:t></a:r>
              <a:endParaRPr lang="zh-CN" sz="{size * 100}"/>
            </a:p>
          </p:txBody>
        </p:sp>"""

    def picture_shape(pic: dict, idx: int) -> str:
        nonlocal shape_id
        sid = shape_id
        shape_id += 1
        descr = escape_xml(pic.get("descr", "project screenshot"))
        return f"""
        <p:pic>
          <p:nvPicPr>
            <p:cNvPr id="{sid}" name="Screenshot {idx}" descr="{descr}"/>
            <p:cNvPicPr><a:picLocks noChangeAspect="1"/></p:cNvPicPr><p:nvPr/>
          </p:nvPicPr>
          <p:blipFill><a:blip r:embed="{pic['rid']}"/><a:stretch><a:fillRect/></a:stretch></p:blipFill>
          <p:spPr>
            <a:xfrm><a:off x="{emu(pic['x'])}" y="{emu(pic['y'])}"/><a:ext cx="{emu(pic['w'])}" cy="{emu(pic['h'])}"/></a:xfrm>
            <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
            <a:ln w="12700"><a:solidFill><a:srgbClr val="5B7D58"/></a:solidFill></a:ln>
          </p:spPr>
        </p:pic>"""

    parts = [
        text_shape(title, 55, 26, 650, 32, 22, "223F2B", True),
        text_shape(subtitle, 58, 58, 790, 24, 12, "465D43"),
    ]
    for i, picture in enumerate(pictures, 1):
        parts.append(picture_shape(picture, i))
    if notes:
        y = 410 if len(pictures) == 1 else 430
        parts.append(text_shape(" ".join(notes), 88, y, 790, 42, 13, "304A36", False, "ctr"))

    xml = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="{NS['a']}" xmlns:r="{NS['r']}" xmlns:p="{NS['p']}">
  <p:cSld>
    <p:bg><p:bgPr><a:solidFill><a:srgbClr val="F6F9F3"/></a:solidFill><a:effectLst/></p:bgPr></p:bg>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
      {''.join(parts)}
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>"""
    return xml.encode("utf-8")


def escape_xml(text: str) -> str:
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
        .replace("'", "&apos;")
    )


def rels_xml(targets: list[tuple[str, str]]) -> bytes:
    rels = ET.Element(f"{{{NS['rel']}}}Relationships")
    for rid, target in targets:
        if target.endswith(".xml"):
            rtype = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout"
        else:
            rtype = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image"
        ET.SubElement(rels, f"{{{NS['rel']}}}Relationship", {"Id": rid, "Type": rtype, "Target": target})
    return ET.tostring(rels, encoding="utf-8", xml_declaration=True)


def next_rel_id(root: ET.Element) -> int:
    max_id = 0
    for rel in root.findall("rel:Relationship", NS):
        rid = rel.attrib.get("Id", "")
        if rid.startswith("rId") and rid[3:].isdigit():
            max_id = max(max_id, int(rid[3:]))
    return max_id + 1


def add_slide_override(content_types: ET.Element, slide_no: int):
    part = f"/ppt/slides/slide{slide_no}.xml"
    for child in content_types.findall("ct:Override", NS):
        if child.attrib.get("PartName") == part:
            return
    ET.SubElement(
        content_types,
        f"{{{NS['ct']}}}Override",
        {
            "PartName": part,
            "ContentType": "application/vnd.openxmlformats-officedocument.presentationml.slide+xml",
        },
    )


def patch_slide_text(xml_bytes: bytes, slide_no: int) -> bytes:
    if slide_no not in {5, 7}:
        return xml_bytes
    root = ET.fromstring(xml_bytes)
    for t in root.findall(".//a:t", NS):
        if t.text == "20 ":
            t.text = "16+4 "
        elif slide_no == 5 and t.text == "张业务表 ":
            t.text = "张表 "
    return ET.tostring(root, encoding="utf-8", xml_declaration=True)


def main():
    if not SOURCE.exists():
        raise FileNotFoundError(SOURCE)
    OUT.parent.mkdir(parents=True, exist_ok=True)

    slides_to_add = [
        {
            "slide_no": 13,
            "title": "运行截图：首页看板支撑治理总览",
            "subtitle": "对齐报告中的治理态势说明，展示资产、敏感字段、待审批申请、风险告警和分类覆盖率。",
            "images": [("01_dashboard.png", "rId2", 80, 86, 800, 450)],
        },
        {
            "slide_no": 14,
            "title": "运行截图：Agent 工作台体现智能治理入口",
            "subtitle": "对齐项目新增 Agent 模块，展示字段分类、审批建议、风险告警和安全报告的一站式入口。",
            "images": [("03_agent_workbench.png", "rId2", 80, 86, 800, 450)],
        },
        {
            "slide_no": 15,
            "title": "运行截图：安全报告与悬浮助手",
            "subtitle": "对齐答辩演示流程的最后一步，展示报告生成和任意页面问答能力。",
            "images": [
                ("07_security_report_generated.png", "rId2", 45, 116, 420, 236),
                ("06_floating_ai.png", "rId3", 495, 116, 420, 236),
            ],
            "notes": [
                "报告页把资产概览、分类覆盖率、风险告警和整改建议生成到同一视图；",
                "悬浮助手提供任意页面可用的本地规则问答入口。",
            ],
        },
    ]

    with ZipFile(SOURCE, "r") as zin:
        entries = {name: zin.read(name) for name in zin.namelist()}

    pres_root = ET.fromstring(entries["ppt/presentation.xml"])
    rel_root = ET.fromstring(entries["ppt/_rels/presentation.xml.rels"])
    ct_root = ET.fromstring(entries["[Content_Types].xml"])

    sld_id_lst = pres_root.find("p:sldIdLst", NS)
    if sld_id_lst is None:
        raise RuntimeError("presentation.xml missing p:sldIdLst")
    existing_ids = [int(el.attrib["id"]) for el in sld_id_lst.findall("p:sldId", NS)]
    next_slide_id = max(existing_ids) + 1
    next_rid = next_rel_id(rel_root)

    new_sld_ids = []
    for spec in slides_to_add:
        slide_no = spec["slide_no"]
        pres_rid = f"rId{next_rid}"
        next_rid += 1
        ET.SubElement(
            rel_root,
            f"{{{NS['rel']}}}Relationship",
            {
                "Id": pres_rid,
                "Type": "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide",
                "Target": f"slides/slide{slide_no}.xml",
            },
        )
        new_el = ET.Element(
            f"{{{NS['p']}}}sldId",
            {"id": str(next_slide_id), f"{{{NS['r']}}}id": pres_rid},
        )
        next_slide_id += 1
        new_sld_ids.append(new_el)
        add_slide_override(ct_root, slide_no)

        pictures = []
        rel_targets = [("rId1", "../slideLayouts/slideLayout1.xml")]
        for image_name, rid, x, y, w, h in spec["images"]:
            src = SHOT_DIR / image_name
            if not src.exists():
                raise FileNotFoundError(src)
            media_name = f"ppt/media/screenshot_{slide_no}_{image_name}"
            entries[media_name] = src.read_bytes()
            rel_targets.append((rid, f"../media/screenshot_{slide_no}_{image_name}"))
            pictures.append({"rid": rid, "x": x, "y": y, "w": w, "h": h, "descr": image_name})

        entries[f"ppt/slides/slide{slide_no}.xml"] = slide_xml(
            spec["title"],
            spec["subtitle"],
            pictures,
            spec.get("notes"),
        )
        entries[f"ppt/slides/_rels/slide{slide_no}.xml.rels"] = rels_xml(rel_targets)

    # Place screenshots immediately after the current "core modules" slide.
    current_sld_ids = list(sld_id_lst)
    insert_after = 4
    for child in current_sld_ids:
        sld_id_lst.remove(child)
    for idx, child in enumerate(current_sld_ids, start=1):
        sld_id_lst.append(child)
        if idx == insert_after:
            for new_el in new_sld_ids:
                sld_id_lst.append(new_el)

    for slide_no in [5, 7]:
        key = f"ppt/slides/slide{slide_no}.xml"
        entries[key] = patch_slide_text(entries[key], slide_no)

    entries["ppt/presentation.xml"] = ET.tostring(pres_root, encoding="utf-8", xml_declaration=True)
    entries["ppt/_rels/presentation.xml.rels"] = ET.tostring(rel_root, encoding="utf-8", xml_declaration=True)
    entries["[Content_Types].xml"] = ET.tostring(ct_root, encoding="utf-8", xml_declaration=True)

    with ZipFile(OUT, "w", ZIP_DEFLATED) as zout:
        for name, data in entries.items():
            zout.writestr(name, data)

    print(OUT)


if __name__ == "__main__":
    main()

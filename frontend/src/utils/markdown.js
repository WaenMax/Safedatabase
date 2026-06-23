const escapeMap = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;'
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, char => escapeMap[char])
}

function renderInline(value) {
  return escapeHtml(value)
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
}

export function renderMarkdown(value) {
  const lines = String(value ?? '').replace(/\r\n/g, '\n').split('\n')
  const html = []
  let listType = null

  function closeList() {
    if (listType) {
      html.push(`</${listType}>`)
      listType = null
    }
  }

  function openList(type) {
    if (listType === type) return
    closeList()
    html.push(`<${type}>`)
    listType = type
  }

  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) {
      closeList()
      continue
    }

    const heading = trimmed.match(/^(#{1,4})\s+(.+)$/)
    if (heading) {
      closeList()
      const level = heading[1].length
      html.push(`<h${level}>${renderInline(heading[2])}</h${level}>`)
      continue
    }

    const listItem = trimmed.match(/^[-*]\s+(.+)$/)
    if (listItem) {
      openList('ul')
      html.push(`<li>${renderInline(listItem[1])}</li>`)
      continue
    }

    const orderedItem = trimmed.match(/^\d+\.\s+(.+)$/)
    if (orderedItem) {
      openList('ol')
      html.push(`<li>${renderInline(orderedItem[1])}</li>`)
      continue
    }

    closeList()
    html.push(`<p>${renderInline(trimmed)}</p>`)
  }

  closeList()
  return html.join('')
}

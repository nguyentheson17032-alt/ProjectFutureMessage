type Props = {
  page: number
  totalPages: number
  onPage: (page: number) => void
}

export function Pager({ page, totalPages, onPage }: Props) {
  if (totalPages <= 1) return null
  return (
    <nav className="pager" aria-label="Phân trang">
      <button type="button" disabled={page <= 0} onClick={() => onPage(page - 1)}>
        Trang trước
      </button>
      <span className="tabular">
        Trang {page + 1}/{totalPages}
      </span>
      <button type="button" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>
        Trang sau
      </button>
    </nav>
  )
}

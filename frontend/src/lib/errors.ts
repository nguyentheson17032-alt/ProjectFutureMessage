import type { ApiErrorBody, FieldError } from '../types'

const VI: Record<string, string> = {
  VALIDATION_ERROR: 'Dữ liệu chưa hợp lệ. Kiểm tra lại các trường.',
  EMAIL_ALREADY_EXISTS: 'Email này đã được đăng ký.',
  INVALID_CREDENTIALS: 'Email hoặc mật khẩu không đúng.',
  INVALID_TOKEN: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  INVALID_REFRESH_TOKEN: 'Phiên đăng nhập không còn hiệu lực.',
  REFRESH_TOKEN_EXPIRED: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  REFRESH_TOKEN_REUSED: 'Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại.',
  UNAUTHORIZED: 'Bạn cần đăng nhập để tiếp tục.',
  FORBIDDEN: 'Bạn không có quyền thực hiện thao tác này.',
  NOT_ADMIN: 'Chỉ quản trị viên mới được thực hiện thao tác này.',
  USER_DISABLED: 'Tài khoản đã bị vô hiệu hóa.',
  CANNOT_MODIFY_SELF_ROLE: 'Không thể tự đổi quyền của chính mình.',
  LAST_ADMIN: 'Không thể thay đổi quản trị viên cuối cùng.',
  NOTIFICATION_NOT_RETRYABLE: 'Chỉ gửi lại email khi thông báo đang FAILED và thư đã mở khóa.',
  NOT_FOUND: 'Không tìm thấy dữ liệu.',
  MESSAGE_NOT_FOUND: 'Không tìm thấy tin nhắn.',
  MESSAGE_LOCKED: 'Tin nhắn vẫn đang khóa.',
  MESSAGE_NOT_EDITABLE: 'Chỉ sửa hoặc hủy khi tin nhắn còn khóa.',
  MESSAGE_NOT_AVAILABLE: 'Tin nhắn chưa sẵn sàng để mở.',
  NOT_SENDER: 'Chỉ người gửi mới được thực hiện thao tác này.',
  NOT_RECIPIENT: 'Chỉ người nhận mới được mở tin nhắn này.',
  UNLOCK_AT_MUST_BE_FUTURE: 'Thời điểm mở phải ở tương lai.',
  CONCURRENT_MODIFICATION: 'Tin nhắn vừa được thay đổi. Tải lại rồi thử lại.',
  RATE_LIMITED: 'Bạn thao tác quá nhanh. Đợi một lát rồi thử lại.',
  INTERNAL_ERROR: 'Có lỗi hệ thống. Thử lại sau.',
  NETWORK_ERROR: 'Không kết nối được máy chủ. Kiểm tra backend đang chạy.',
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: FieldError[]

  constructor(status: number, code: string, message: string, fieldErrors: FieldError[] = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

export function errorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    return VI[err.code] ?? err.message
  }
  if (err instanceof Error) return err.message
  return 'Có lỗi xảy ra. Thử lại sau.'
}

export function fieldErrorMap(err: unknown): Record<string, string> {
  if (!(err instanceof ApiError) || err.fieldErrors.length === 0) return {}
  return Object.fromEntries(err.fieldErrors.map((item) => [item.field, item.message]))
}

export function parseApiError(status: number, body: unknown): ApiError {
  const data = body as ApiErrorBody | undefined
  if (data && typeof data.code === 'string') {
    return new ApiError(status, data.code, data.message ?? 'Request failed', data.fieldErrors ?? [])
  }
  return new ApiError(status, 'INTERNAL_ERROR', 'Request failed')
}

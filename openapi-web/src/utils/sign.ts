// 与后端 SignatureUtils 一致的签名算法（HMAC-SHA256，浏览器 Web Crypto 实现）
export async function hmacSha256Hex(content: string, secret: string): Promise<string> {
  const encoder = new TextEncoder()
  const key = await crypto.subtle.importKey(
    'raw',
    encoder.encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  )
  const sig = await crypto.subtle.sign('HMAC', key, encoder.encode(content))
  return Array.from(new Uint8Array(sig))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}

export interface SignParams {
  [key: string]: string
}

export function buildSignContent(method: string, path: string, params: SignParams): string {
  const sorted = Object.keys(params)
    .sort()
    .map((k) => `${k}=${params[k] ?? ''}`)
    .join('&')
  return `${method.toUpperCase()}\n${path}\n${sorted}`
}

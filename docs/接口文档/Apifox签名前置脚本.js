// ============================================================
// Apifox 前置操作脚本：为受签名保护的 /api/** 接口生成请求签名
// 使用方式：Apifox 自动化测试 -> 测试场景 -> 前置操作 中粘贴本脚本
// 依赖：Apifox 脚本环境内置 CryptoJS
// ============================================================

const CryptoJS = require('crypto-js');

// 应用密钥（与 db/init.sql 中演示应用一致，正式环境请改用环境变量）
const ACCESS_KEY = 'demo-access-key';
const SECRET_KEY = 'demo-secret-key';

const timestamp = String(Date.now());
const nonce = CryptoJS.lib.WordArray.random(8).toString();

// 1. 收集 query 参数
const params = {};
(pm.request.url.query || []).forEach((q) => {
    if (q && q.key) {
        params[q.key] = q.value === undefined ? '' : String(q.value);
    }
});

// 2. 收集表单参数（POST urlencoded）
if (pm.request.body && pm.request.body.mode === 'urlencoded' && pm.request.body.urlencoded) {
    pm.request.body.urlencoded.forEach((item) => {
        if (item && item.key) {
            params[item.key] = item.value === undefined ? '' : String(item.value);
        }
    });
}

// 3. 加入 timestamp / nonce
params.timestamp = timestamp;
params.nonce = nonce;

// 4. 按 key 字典序拼接
const sorted = Object.keys(params).sort()
    .map((k) => k + '=' + (params[k] == null ? '' : params[k]))
    .join('&');

// 5. 签名内容：METHOD\n请求路径\n排序后的参数
const path = pm.request.url.getPath();
const content = pm.request.method.toUpperCase() + '\n' + path + '\n' + sorted;
const signature = CryptoJS.HmacSHA256(content, SECRET_KEY).toString(CryptoJS.enc.Hex);

// 6. 注入签名请求头
pm.request.headers.upsert({ key: 'X-Access-Key', value: ACCESS_KEY });
pm.request.headers.upsert({ key: 'X-Timestamp', value: timestamp });
pm.request.headers.upsert({ key: 'X-Nonce', value: nonce });
pm.request.headers.upsert({ key: 'X-Signature', value: signature });

console.log('signContent:', content);
console.log('signature:', signature);

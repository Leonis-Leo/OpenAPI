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

const params = {};

function addParam(key, value) {
    const v = value === undefined ? '' : String(value);
    if (v !== '') {
        params[key] = v;
    }
}

function normalizeValue(raw) {
    // Apifox 的 query/form 值可能是 PostmanQueryParam 对象，需要取 .value
    if (raw && typeof raw === 'object' && 'value' in raw) {
        return raw.value;
    }
    return raw;
}

// 1. 收集 query 参数（兼容数组与对象两种形态）
const query = pm.request.url.query;
if (Array.isArray(query)) {
    query.forEach((q) => {
        if (q && q.key) {
            addParam(q.key, normalizeValue(q.value));
        }
    });
} else if (query && typeof query === 'object') {
    Object.keys(query).forEach((k) => {
        if (k.indexOf('_postman') === 0) {
            return; // 跳过 Apifox 内部字段
        }
        addParam(k, normalizeValue(query[k]));
    });
}

// 2. 收集表单参数（兼容数组与对象两种形态）
const body = pm.request.body;
if (body && body.mode === 'urlencoded') {
    const form = body.urlencoded;
    if (Array.isArray(form)) {
        form.forEach((item) => {
            if (item && item.key) {
                addParam(item.key, normalizeValue(item.value));
            }
        });
    } else if (form && typeof form === 'object') {
        Object.keys(form).forEach((k) => {
            if (k.indexOf('_postman') === 0) {
                return;
            }
            addParam(k, normalizeValue(form[k]));
        });
    }
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
function setHeader(key, value) {
    if (typeof pm.request.headers.upsert === 'function') {
        pm.request.headers.upsert({ key: key, value: value });
    } else {
        pm.request.headers.add({ key: key, value: value });
    }
}
setHeader('X-Access-Key', ACCESS_KEY);
setHeader('X-Timestamp', timestamp);
setHeader('X-Nonce', nonce);
setHeader('X-Signature', signature);

console.log('signContent:', content);
console.log('signature:', signature);

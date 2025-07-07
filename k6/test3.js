import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '10m', target: 3000 },  // 10분간 6000 VU로 점진 증가
    ],
};

export default function () {
    const userId    = '11111111-2222-3333-4444-555555555555';
    const articleId = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee';
    const after     = '';
    const limit     = 20;

    let qs = `?articleId=${articleId}&limit=${limit}`;
    if (after) {
        qs += `&after=${encodeURIComponent(after)}`;
    }

    const url = `http://127.0.0.1:8080/api/comments${qs}`;

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Monew-Request-User-ID': userId,
        },
    };

    const res = http.get(url, params);

    check(res, { '✅ status is 200': (r) => r.status === 200 });
}
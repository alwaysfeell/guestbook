<#include "layout/header.ftl">
<h2 style="margin:0 0 16px;font-size:22px;color:#1e293b;">Підтвердження реєстрації</h2>
<p style="margin:0 0 12px;">Вітаємо, <b>${username}</b>!</p>
<p style="margin:0 0 12px;">Для адреси <b>${email}</b> створено обліковий запис у каталозі книг з роллю <b>USER</b>. Щоб увійти, підтвердіть реєстрацію:</p>
<p style="margin:24px 0;">
<a href="${confirmUrl}" style="display:inline-block;background:#2563eb;color:#ffffff;padding:10px 18px;text-decoration:none;border-radius:6px;font-weight:600;">Підтвердити акаунт</a>
</p>
<p style="margin:0 0 6px;color:#64748b;font-size:13px;">Якщо кнопка не працює, відкрийте посилання в браузері:</p>
<p style="margin:0 0 16px;font-size:13px;word-break:break-all;"><a href="${confirmUrl}" style="color:#2563eb;">${confirmUrl}</a></p>
<p style="margin:0 0 16px;color:#64748b;font-size:13px;">Дата реєстрації: ${registered?string("yyyy-MM-dd HH:mm")}. Якщо ви не реєструвалися, просто проігноруйте цей лист.</p>
<#include "layout/footer.ftl">

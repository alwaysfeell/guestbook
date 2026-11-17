<#include "layout/header.ftl">
<h2 style="margin:0 0 16px;font-size:22px;color:#1e293b;">Нова книга додана до каталогу</h2>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#eff6ff;border:1px solid #bfdbfe;border-left:4px solid #2563eb;border-radius:6px;">
<tr>
<td style="padding:16px 20px;">
<p style="margin:0 0 6px;">Назва: <b>${title}</b></p>
<p style="margin:0 0 6px;">Автор: <b>${author}</b></p>
<p style="margin:0 0 6px;">Рік видання: <b>${year}</b></p>
<p style="margin:0;">Номер у каталозі: <b>${id}</b></p>
<#if year < 2000>
<p style="margin:12px 0 0;padding:8px 12px;background:#fef3c7;border:1px solid #fcd34d;border-radius:4px;color:#a36b00;"><b>Раритетне видання!</b> Книзі вже ${added?string("yyyy")?number - year} р.</p>
</#if>
</td>
</tr>
</table>
<p style="margin:24px 0;">
<a href="${bookUrl}" style="display:inline-block;background:#2563eb;color:#ffffff;padding:10px 18px;text-decoration:none;border-radius:6px;font-weight:600;">Переглянути книгу</a>
</p>
<#if recentBooks?has_content>
<p style="margin:0 0 6px;font-weight:600;color:#1e293b;">Нещодавно в каталозі:</p>
<ul style="margin:0 0 16px;padding-left:20px;">
<#list recentBooks as b>
<li style="margin-bottom:4px;">${b.title} &mdash; ${b.author}, ${b.pubYear}</li>
</#list>
</ul>
</#if>
<p style="margin:0 0 4px;">Усього книг у каталозі: <b>${totalBooks}</b></p>
<p style="margin:0 0 16px;color:#64748b;font-size:13px;">Дата додавання: ${added?string("yyyy-MM-dd HH:mm")}</p>
<#include "layout/footer.ftl">

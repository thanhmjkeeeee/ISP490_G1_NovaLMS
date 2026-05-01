
import sys

path = r'c:\Users\huy\Desktop\New code\ISP490_G1_NovaLMS\NovaLMS\DoAn\src\main\resources\templates\teacher\workspace.html'

with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

target = 'return `<div class="bg-light p-2 rounded small mb-2"><strong>Trả lời:</strong> ${wsEscape(ans)}</div>`;'
replacement = '''return `
                                             <div class="rounded-4 p-3 mb-3" style="background: #f8fafc; border: 1px solid #e2e8f0;">
                                                <div class="extra-small fw-bold text-muted text-uppercase mb-2" style="font-size: 0.6rem; letter-spacing: 0.5px;">Student Response</div>
                                                <div class="text-dark small">${wsEscape(ans)}</div>
                                             </div>`;'''

if target in content:
    new_content = content.replace(target, replacement)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print("Success")
else:
    print("Target not found")

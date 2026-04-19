
def check_braces(filename):
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()
    
    stack = []
    in_script = False
    script_content = ""
    lines = content.split('\n')
    
    for i, line in enumerate(lines):
        if '<script th:inline="javascript">' in line:
            in_script = True
            continue
        if '</script>' in line and in_script:
            in_script = False
            # Check braces in script_content
            s_stack = []
            for j, char in enumerate(script_content):
                if char == '{':
                    s_stack.append(j)
                elif char == '}':
                    if s_stack:
                        s_stack.pop()
                    else:
                        print(f"Extra closing brace in script starting at line {i-len(script_content.splitlines())}")
            if s_stack:
                print(f"Unclosed braces in script: {len(s_stack)}")
            script_content = ""
            continue
        
        if in_script:
            script_content += line + '\n'

check_braces('c:\\Users\\huy\\Desktop\\New folder\\ISP490_G1_NovaLMS\\NovaLMS\\DoAn\\src\\main\\resources\\templates\\expert\\quiz-edit.html')

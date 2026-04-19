
const fs = require('fs');
const content = fs.readFileSync('c:\\Users\\huy\\Desktop\\New folder\\ISP490_G1_NovaLMS\\NovaLMS\\DoAn\\src\\main\\resources\\templates\\expert\\quiz-edit.html', 'utf8');

let inScript = false;
let scriptContent = "";
let scriptStartLine = 0;
const lines = content.split('\n');

for (let i = 0; i < lines.length; i++) {
    if (lines[i].includes('<script th:inline="javascript">')) {
        inScript = true;
        scriptStartLine = i + 1;
        continue;
    }
    if (lines[i].includes('</script>') && inScript) {
        inScript = false;
        checkBraces(scriptContent, scriptStartLine);
        scriptContent = "";
        continue;
    }
    if (inScript) {
        scriptContent += lines[i] + '\n';
    }
}

function checkBraces(code, startLine) {
    let balance = 0;
    const lines = code.split('\n');
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        // Ignore braces inside strings and comments (simplistic)
        let cleanLine = line.replace(/\/\/.*/, '').replace(/\/\*.*?\*\//g, '').replace(/'(.*?)'/g, "''").replace(/"(.*?)"/g, '""').replace(/`(.*?)`/g, '``');
        for (let char of cleanLine) {
            if (char === '{') balance++;
            if (char === '}') balance--;
        }
        if (balance < 0) {
            console.log(`Negative balance at line ${startLine + i}: ${balance}`);
        }
    }
    console.log(`Final balance for script starting at line ${startLine}: ${balance}`);
}

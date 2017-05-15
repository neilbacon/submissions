const fs = require('fs');

// call f with all of stdin as a string as its arg
function doWithStream(s, fn) {
    var buf = '';
    s.setEncoding('utf8');
    s.on('data', data => buf += data.toString());
    s.on('end', () => fn(buf));
}

function writeDocIn(docIn) {
  process.stdout.write(JSON.stringify(docIn) + '\n');
}

process.stdout.setDefaultEncoding('utf8');

// args specify paths of text files
// write each path and content of these files as a JSON DocIn on one line (suitable for input to dataFusion-ner)
process.argv.slice(2).forEach(path => {
  doWithStream(fs.createReadStream(path), content => writeDocIn({path: path, content: content}));
});

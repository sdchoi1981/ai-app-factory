import {execFileSync} from 'node:child_process';
import {mkdtempSync,rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
const dir=mkdtempSync(join(tmpdir(),'datekit-'));
try{
 execFileSync('javac',['-encoding','UTF-8','-d',dir,'app/src/main/java/com/factory/datekit/DateMath.java','test/DateMathTest.java'],{stdio:'inherit'});
 execFileSync('java',['-cp',dir,'DateMathTest'],{stdio:'inherit'});
}finally{rmSync(dir,{recursive:true,force:true});}

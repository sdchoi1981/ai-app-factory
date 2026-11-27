package factory.kit;
import java.io.*;
/** GIF89a with deterministic 3-3-2 palette, fixed-width literal LZW blocks. */
final class GifWriter implements AutoCloseable {
 private final OutputStream out;private final int w,h;private boolean closed;
 GifWriter(OutputStream out,int w,int h)throws IOException{if(w<1||w>65535||h<1||h>65535)throw new IllegalArgumentException();this.out=out;this.w=w;this.h=h;out.write(new byte[]{'G','I','F','8','9','a'});word(w);word(h);out.write(0xf7);out.write(0);out.write(0);for(int i=0;i<256;i++){out.write(((i>>5)&7)*255/7);out.write(((i>>2)&7)*255/7);out.write((i&3)*255/3);}out.write(new byte[]{0x21,(byte)0xff,11,'N','E','T','S','C','A','P','E','2','.','0',3,1,0,0,0});}
 private void word(int n)throws IOException{out.write(n&255);out.write(n>>8&255);}
 void frame(int[] p,int delay)throws IOException{if(closed||p.length!=w*h||delay<10||delay>655350)throw new IllegalArgumentException();out.write(new byte[]{0x21,(byte)0xf9,4,4});word(Math.max(1,delay/10));out.write(0);out.write(0);out.write(0x2c);word(0);word(0);word(w);word(h);out.write(0);out.write(8);Bits bits=new Bits();int n=0;bits.put(256);for(int v:p){if(n==200){bits.put(256);n=0;}bits.put(((v>>16)&0xe0)|((v>>11)&0x1c)|((v>>6)&3));n++;}bits.put(257);bits.finish();out.write(0);}
 private class Bits{int buffer,bits,len;byte[] block=new byte[255];void put(int code)throws IOException{buffer|=code<<bits;bits+=9;while(bits>=8){emit(buffer&255);buffer>>>=8;bits-=8;}}void emit(int b)throws IOException{block[len++]=(byte)b;if(len==255)flush();}void flush()throws IOException{if(len>0){out.write(len);out.write(block,0,len);len=0;}}void finish()throws IOException{if(bits>0)emit(buffer&255);flush();}}
 public void close()throws IOException{if(!closed){closed=true;out.write(0x3b);out.flush();}}
}

package factory.fun;
final class Rules{static float position(float screen,float width,int direction,float distance){if(direction==2)return (screen-width)/2;float total=Math.max(1,screen+width);float d=((distance%total)+total)%total;return direction==0?screen-d:d-width;}}

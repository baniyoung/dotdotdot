#include "braille.h"
#include <SoftwareSerial.h>

SoftwareSerial mySerial(7, 6);
int dataPin = 2; // DATA 핀번호
int latchPin= 3; // LATCH 핀번호
int clockPin = 4; // CLOCK 핀번호
int no_module = 1; // 점자 출력기 연결 개수

braille bra(dataPin, latchPin, clockPin, no_module);

char string_buffer[100];//블루투스로부터 수신받은 문자열
char string_buffer_serial[100][4];//수신받은 문자열을 글자 단위로 분리하여 배열에 저장

int str_char_count=0;

byte hangul_cho[20]={
  0b00010000,//ㄱ
  0b00010000,//ㄲ
  0b00110000,//ㄴ
  0b00011000,//ㄷ
  0b00011000,//ㄸ
  0b00000100,//ㄹ
  0b00100100,//ㅁ
  0b00010100,//ㅂ
  0b00010100,//ㅃ
  0b00000001,//ㅅ
  0b00000001,//ㅆ
  0b00001111,//ㅇ
  0b00010001,//ㅈ
  0b00010001,//ㅉ
  0b00000101,//ㅊ
  0b00111000,//ㅋ
  0b00101100,//ㅌ
  0b00110100,//ㅍ
  0b00011100,//ㅎ
  0b00000000//없는 거
};//점자 표시 데이터
//초성 코드번호
//ㄱ,ㄲ,ㄴ,ㄷ,ㄸ,ㄹ,ㅁ,ㅂ,ㅃ,ㅅ,ㅆ,ㅇ,ㅈ,ㅉ,ㅊ,ㅋ,ㅌ,ㅍ,ㅎ, 없는거 
byte hangul2_cho[20]={0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};

byte hangul_jung[21]={
  0b00101001,//ㅏ
  0b00101110,//ㅐ
  0b00010110,//ㅑ
  0b00010110,//ㅒ
  0b00011010,//ㅓ
  0b00110110,//ㅔ
  0b00100101,//ㅕ
  0b00010010,//ㅖ
  0b00100011,//ㅗ
  0b00101011,//ㅘ
  0b00101011,//ㅙ
  0b00110111,//ㅚ
  0b00010011,//ㅛ
  0b00110010,//ㅜ
  0b00111010,//ㅝ
  0b00111010,//ㅞ
  0b00110010,//ㅟ
  0b00110001,//ㅠ
  0b00011001,//ㅡ
  0b00011101,//ㅢ
  0b00100110//ㅣ
};

byte hangul2_jung[21]={0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};

byte hangul_jong[28]={
  0b00000000,//없음
  0b00100000,//ㄱ
  0b00100000,//ㄲ
  0b00100000,//ㄱㅅ
  0b00001100,//ㄴ
  0b00001100,//ㄴㅈ
  0b00001100,//ㄴㅎ
  0b00000110,//ㄷ
  0b00001000,//ㄹ
  0b00001000,//ㄹㄱ
  0b00001000,//ㄹㅁ
  0b00001000,//ㄹㅂ
  0b00001000,//ㄹㅅ
  0b00001000,//ㄹㅌ
  0b00001000,//ㄹㅍ
  0b00001000,//ㄹㅎ
  0b00001001,//ㅁ
  0b00101000,//ㅂ
  0b00101000,//ㅂㅅ
  0b00000010,//ㅅ
  0b00000010,//ㅆ
  0b00001111,//ㅇ
  0b00100010,//ㅈ
  0b00001010,//ㅊ
  0b00001110,//ㅋ
  0b00001011,//ㅌ
  0b00001101,//ㅍ
  0b00000111//ㅎ
};
byte hangul2_jong[28]={0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27};
void setup() 
{
  Serial.begin(9600);
  mySerial.begin(9600);
  bra.begin();
  delay(1000);
  bra.all_off();
  bra.refresh();
}

void loop() 
{
  
  if(mySerial.available())
  {
    String str=mySerial.readStringUntil('\n');
    str.replace("\r","");
    strcpy(string_buffer,str.c_str());
    {
      int ind=0;
      int len=strlen(string_buffer);
      int index=0;
      while(ind<len)
      {
        int bytes=get_char_byte(string_buffer+ind);
        if(bytes==3)
        {
          string_buffer_serial[index][0]=*(string_buffer+ind);
          string_buffer_serial[index][1]=*(string_buffer+ind+1);
          string_buffer_serial[index][2]=*(string_buffer+ind+2);
          string_buffer_serial[index][3]=0;
          index++;
        }
        ind+=bytes;
      }
      str_char_count=index;
    }
    int ind=0;
    for(int i=0;i<str_char_count;i++)
    {
      Serial.println((char*)(string_buffer_serial+ind));
      if(string_buffer_serial[i][1]!=0)
      {
        unsigned int cho, jung, jong;
        split_han_cho_jung_jong(string_buffer_serial[i][0], string_buffer_serial[i][1], string_buffer_serial[i][2], cho, jung, jong);
        han_braille_cho(cho);
        delay(300);
        han_braille_jung(jung);
        delay(300);
        han_braille_jong(jong);
        delay(300);
        bra.all_off();
        bra.refresh();
        delay(300);
        //Serial.print(hangul_cho[cho], BIN);
        //Serial.print(",");
        //Serial.print(hangul_jung[jung], BIN);
        //Serial.print(",");
        //Serial.print(hangul_jong[jong], BIN);
        //Serial.print("\n");
        ind=ind+1;
      }
    }
    Serial.println("###");
    
  }
}

unsigned char get_char_byte(char *pos)
{
  char val=*pos;
  #ifdef DEBUG
  Serial.println("####");
  Serial.println(val, BIN);
  Serial.println("####");
  #endif
  if((val&0b10000000)==0)
  {
    return 1;
  }
  else if((val&0b11100000)==0b11000000)
  {
    return 2;
  }
  else if((val&0b11110000)==0b11100000)
  {
    return 3;
  }
  else if((val&0b11111000)==0b11110000)
  {
    return 4;
  }
  else if((val&0b11111100)==0b11111000)
  {
    return 5;
  }
  else
  {
    return 6;
  }
}

/*void ascii_braille(int code)
{
  bra.all_off();
  for(int i=0;i<6;i++)
  {
    int on_off=ascii_data[code]>>(5-i)&0b00000001;
    if(on_off != 0)
    {
      bra.on(0,i);
    }
    else
    {
      bra.off(0,i);
    }
  }
  bra.refresh();
}*/

void split_han_cho_jung_jong(char byte1, char byte2, char byte3, unsigned int &cho, unsigned int &jung, unsigned int &jong)
{
  unsigned int utf16=(byte1&0b00001111)<<12|(byte2&0b00111111)<<6|(byte3&0b00111111);
  unsigned int val=utf16-0xac00;

  unsigned char _jong=val%28;
  unsigned char _jung=(val%(28*21))/28;
  unsigned char _cho=val/(28*21);

  cho=0;
  for(int i=0;i<19;i++)
  {
    if(_cho==hangul2_cho[11])
    {
      cho=20;
    }
    if(_cho==hangul2_cho[i])
    {
      cho=i;
    }
  }
  jung=0;
  for(int i=0;i<21;i++)
  {
    if(_jung==hangul2_jung[i])
    {
      jung=i;
    }
  }
  jong=0;
  for(int i=0;i<28;i++)
  {
    if(_jong==hangul2_jong[i])
    {
      jong=i;
    }
  }
}

void han_braille_cho(int index1)
{
  bra.all_off();
  for(int i=0;i<6;i++)
  {
    int on_off=hangul_cho[index1]>>(5-i)&0b00000001;
    if(on_off!=0)
    {
      bra.on(0,i);
    }
    else
    {
      bra.off(0,i);
    }
  }
  bra.refresh();
}
void han_braille_jung(int index2)
{
  bra.all_off();
  for(int i=0; i<6;i++)
  {
    int on_off=hangul_jung[index2]>>(5-i)&0b00000001;
    if(on_off!=0)
    {
      bra.on(0,i);
    }
    else
    {
      bra.off(0,i);
    }
  }
  bra.refresh();
}
void han_braille_jong(int index3)
{
  bra.all_off();
  for(int i=0;i<6;i++)
  {
    int on_off=hangul_jong[index3]>>(5-i)&0b00000001;
    if(on_off!=0)
    {
      bra.on(0,i);
    }
    else
    {
      bra.off(0,i);
    }
  }
  bra.refresh();
}

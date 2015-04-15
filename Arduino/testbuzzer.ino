#include <RFduinoBLE.h>
boolean b=false, bCon=false;
const int btn = 5, buz = 2;
int prev=0;

void setup() {
  pinMode(btn, INPUT); 
  RFduinoBLE.advertisementData = "buz";
  RFduinoBLE.begin();
  prev=millis();
}

void loop() {
  if(b)  {
    tone(buz, 200);
    delay(100);
    noTone(buz);
    b=false;
  }

  bool val = digitalRead(btn);
  if(bCon && val &&  millis() - prev >= 1000) {
    RFduinoBLE.sendByte(1);
    prev=millis();
  }
}

void RFduinoBLE_onConnect() {  bCon=true; }
void RFduinoBLE_onDisconnect() {  bCon=false; }
void RFduinoBLE_onReceive(char *data, int len) {
  if (len==1 && data[0]==1) b=true;
}

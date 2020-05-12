package olsenn1.Messages.Light;

import olsenn1.LifxCommander.CommonMethods;
import olsenn1.Messages.DataTypes.Payload;

public class StateInfrared extends Payload {
	int code = 121;
	int brightness;			// 16-Bits (Unsigned)
	
	public StateInfrared() {
		brightness = 0;
	}
	
	public StateInfrared(int brightness) {
		this.brightness = brightness;
	}
	
	public StateInfrared(StateInfrared stateInfrared) {
		brightness = stateInfrared.brightness;
	}
	
	public int getCode() {
		return code;
	}
	
	public int getBrightness() {
		return brightness;
	}
	
	public void setBrightness(int brightness) {
		this.brightness = brightness;
	}
	
	public void setFromCommandByteArray(byte[] byteArray) {
		String brightnessBinStr = CommonMethods.convertByteToBinaryString(byteArray[36]);
		brightness = Integer.parseInt(brightnessBinStr, 2);
	}
}

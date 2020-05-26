package olsenn1.Messages.Light;

import olsenn1.LifxCommander.CommonMethods;
import olsenn1.Messages.DataTypes.Payload;

public class StatePower_Light extends Payload{
	int code = 118;
	int level;				// 16-Bits (Unsigned)
	
	public StatePower_Light() {
		level = 0;
	}
	
	public StatePower_Light(int level) {
		this.level = level;
	}
	
	public StatePower_Light(StatePower_Light statePower) {
		level = statePower.level;
	}
	
	public int getCode() {
		return code;
	}
	
	public int getLevel() {
		return level;
	}
	
	public void setLevel(int level) {
		this.level = level;
	}
	
	public void setFromCommandByteArray(byte[] byteArray) {
		String levelBinStr = CommonMethods.convertByteToBinaryString(byteArray[37]).concat(CommonMethods.convertByteToBinaryString(byteArray[36]));
		level = Integer.parseInt(levelBinStr, 2);
	}
}
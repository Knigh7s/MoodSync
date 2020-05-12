package olsenn1.Messages.Light;

import olsenn1.Messages.DataTypes.Payload;

public class GetPower_Light extends Payload{
	int code = 116;
	
	public GetPower_Light() {}
	
	public int getCode() {
		return code;
	}
}

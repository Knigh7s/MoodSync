package olsenn1.Messages.Light;

import olsenn1.Messages.DataTypes.Payload;

public class Get extends Payload{
	int code = 101;
	
	public Get() {}
	
	public int getCode() {
		return code;
	}
}

package olsenn1.Messages.Light;

import olsenn1.Messages.DataTypes.Payload;

public class GetInfrared extends Payload {
	int code = 120;
	
	public GetInfrared() {}
	
	public int getCode() {
		return code;
	}
}

package olsenn1.Messages.Light;

import olsenn1.LifxCommander.CommonMethods;
import olsenn1.Messages.DataTypes.*;

public class SetColor extends Payload {
	int code = 102;
	int reserved;			// 8-Bits
	HSBK hsbk;
	long duration;			// 32-Bits (Unsigned)
	int index;				// 16-Bits (Unsigned)
	int colorsCount;		// 8-Bits (Unsigned)
	HSBK[] colors;
	int apply;				// 8-bits (Unsigned)
	int multicolorDirection = 1;

	public SetColor() {
		reserved = 0;
		hsbk = new HSBK();
		duration = 0;
	}
	
	public SetColor(int reserved, HSBK hsbk, long duration) {
		this.reserved = reserved;
		this.hsbk = hsbk;
		this.duration = duration;
	}
	
	public SetColor(HSBK hsbk, long duration) {
		reserved = 0;
		this.hsbk = hsbk;
		this.duration = duration;
	}
	
	public SetColor(HSBK hsbk) {
		reserved = 0;
		this.hsbk = hsbk;
		duration = 0;
	}

	public SetColor(HSBK[] colors, long duration, int direction){
		reserved = 0;
		this.code = 510;
		this.hsbk = null;
		this.duration = duration;
		this.apply = 1;
		this.index = 0;
		this.colorsCount = colors.length;
		if(direction < 0) {
			this.multicolorDirection = -1;
		}
		this.colors = colors;
	}
	
	public int getCode() {
		return code;
	}
	
	public int getReserved() {
		return reserved;
	}
	
	public void setReserved(int reserved) {
		this.reserved = reserved;
	}
	
	public HSBK getHsbk() {
		return hsbk;
	}
	
	public void setHsbk(HSBK hsbk) {
		this.hsbk = hsbk;
	}
	
	public long getDuration() {
		return duration;
	}
	
	public void setDuration(long duration) {
		this.duration = duration;
	}
	
	public byte[] getByteArray() {
		if (this.hsbk != null) {
			byte[] byteArray = new byte[13];

			byte[] reservedByte = new byte[1];
			String reservedBinStr = String.format("%8s", Integer.toBinaryString(reserved)).replace(' ', '0');
			reservedByte = CommonMethods.convertBinaryStringToLittleEndianByteArray(reservedBinStr);
			byteArray[0] = reservedByte[0];

			byte[] hueBytes = new byte[2];
			String hueBinStr = String.format("%16s", Integer.toBinaryString(hsbk.getHue())).replace(' ', '0');
			hueBytes = CommonMethods.convertBinaryStringToLittleEndianByteArray(hueBinStr);
			byteArray[1] = hueBytes[0];
			byteArray[2] = hueBytes[1];

			byte[] saturationBytes = new byte[2];
			String saturationBinStr = String.format("%16s", Integer.toBinaryString(hsbk.getSaturation())).replace(' ', '0');
			saturationBytes = CommonMethods.convertBinaryStringToLittleEndianByteArray(saturationBinStr);
			byteArray[3] = saturationBytes[0];
			byteArray[4] = saturationBytes[1];

			byte[] brightnessBytes = new byte[2];
			String brightnessBinStr = String.format("%16s", Integer.toBinaryString(hsbk.getBrightness())).replace(' ', '0');
			brightnessBytes = CommonMethods.convertBinaryStringToLittleEndianByteArray(brightnessBinStr);
			byteArray[5] = brightnessBytes[0];
			byteArray[6] = brightnessBytes[1];

			byte[] kelvinBytes = new byte[2];
			String kelvinBinStr = String.format("%16s", Integer.toBinaryString(hsbk.getKelvin())).replace(' ', '0');
			kelvinBytes = CommonMethods.convertBinaryStringToLittleEndianByteArray(kelvinBinStr);
			byteArray[7] = kelvinBytes[0];
			byteArray[8] = kelvinBytes[1];

			byte[] durationBytes = new byte[4];
			String durationBinStr = String.format("%32s", Long.toBinaryString(duration)).replace(' ', '0');
			durationBytes = CommonMethods.convertBinaryStringToLittleEndianByteArray(durationBinStr);
			byteArray[9] = durationBytes[0];
			byteArray[10] = durationBytes[1];
			byteArray[11] = durationBytes[2];
			byteArray[12] = durationBytes[3];

			return byteArray;
		} else {
			return getMultizoneByteArray();
		}
	}

	private byte[] getHSBKBytes(){
		byte[] byteArray = new byte[8*colorsCount];
		int start = 0;
		int end = colorsCount;
		switch (this.multicolorDirection){
			case 1:
				start = 0;
				end = colorsCount;
				break;
			case -1:
				start = colorsCount-1;
				end = -1;
				break;
		}
		int counter=0;
		for (int i=start; i!=end; i+=multicolorDirection) {
			byte[] hueBytes = new byte[2];
			String hueBinStr = String.format("%16s", Integer.toBinaryString(colors[i].getHue())).replace(' ', '0');
			hueBytes = CommonMethods.convertBinaryStringToLittleEndianByteArray(hueBinStr);
			byteArray[0+counter*8] = hueBytes[0];
			byteArray[1+counter*8] = hueBytes[1];

			byte[] saturationBytes = new byte[2];
			String saturationBinStr = String.format("%16s", Integer.toBinaryString(colors[i].getSaturation())).replace(' ', '0');
			saturationBytes = CommonMethods.convertBinaryStringToLittleEndianByteArray(saturationBinStr);
			byteArray[2+counter*8] = saturationBytes[0];
			byteArray[3+counter*8] = saturationBytes[1];

			byte[] brightnessBytes = new byte[2];
			String brightnessBinStr = String.format("%16s", Integer.toBinaryString(colors[i].getBrightness())).replace(' ', '0');
			brightnessBytes = CommonMethods.convertBinaryStringToLittleEndianByteArray(brightnessBinStr);
			byteArray[4+counter*8] = brightnessBytes[0];
			byteArray[5+counter*8] = brightnessBytes[1];

			byte[] kelvinBytes = new byte[2];
			String kelvinBinStr = String.format("%16s", Integer.toBinaryString(colors[i].getKelvin())).replace(' ', '0');
			kelvinBytes = CommonMethods.convertBinaryStringToLittleEndianByteArray(kelvinBinStr);
			byteArray[6+counter*8] = kelvinBytes[0];
			byteArray[7+counter*8] = kelvinBytes[1];
			counter++;
		}
		return byteArray;
	}

	private byte[] getMultizoneByteArray() {
		byte[] colorsByteArray = getHSBKBytes();
		byte[] byteArray = new byte[8+colorsByteArray.length];

		byte[] durationBytes = new byte[4];
		String durationBinStr = String.format("%32s", Long.toBinaryString(duration)).replace(' ', '0');
		durationBytes = CommonMethods.convertBinaryStringToLittleEndianByteArray(durationBinStr);
		byteArray[0] = durationBytes[0];
		byteArray[1] = durationBytes[1];
		byteArray[2] = durationBytes[2];
		byteArray[3] = durationBytes[3];

		byte[] applyByte = new byte[1];
		String applyBinStr = String.format("%8s", Integer.toBinaryString(apply)).replace(' ', '0');
		applyByte = CommonMethods.convertBinaryStringToLittleEndianByteArray(applyBinStr);
		byteArray[4] = applyByte[0];

		byte[] indexBytes = new byte[2];
		String indexBinStr = String.format("%16s", Integer.toBinaryString(index)).replace(' ', '0');
		indexBytes = CommonMethods.convertBinaryStringToLittleEndianByteArray(indexBinStr);
		byteArray[5] = indexBytes[0];
		byteArray[6] = indexBytes[1];

		byte[] colorsCountByte = new byte[1];
		String colorsCountBinStr = String.format("%8s", Integer.toBinaryString(colorsCount)).replace(' ', '0');
		colorsCountByte = CommonMethods.convertBinaryStringToLittleEndianByteArray(colorsCountBinStr);
		byteArray[7] = colorsCountByte[0];


		for (int i=0; i<colorsByteArray.length; i++){
			byteArray[8+i] = colorsByteArray[i];
		}

		return byteArray;
	}
}

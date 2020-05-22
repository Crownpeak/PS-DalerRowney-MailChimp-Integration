package com.dalerrowney;

public class Logger {
	private String _final = null;
	private StringBuffer _buffer = new StringBuffer(1024);
	
	public void log(String message) {
		if(_buffer != null)
			_buffer.append(message).append(System.getProperty("line.separator"));
	}
	
	public String toString() {
		if(_final == null) { 
			_final = _buffer.toString();
			_buffer = null;
		}
		return _final;
	}
}

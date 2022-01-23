package com.zanclus;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

import java.time.LocalDate;

public class LocalDateCodec implements MessageCodec<LocalDate, String> {
	
	/**
	 * Called by Vert.x when marshalling a message to the wire.
	 *
	 * @param buffer    the message should be written into this buffer
	 * @param localDate the message that is being sent
	 */
	@Override
	public void encodeToWire(Buffer buffer, LocalDate localDate) {
		buffer.appendString(localDate.toString());
	}
	
	/**
	 * Called by Vert.x when a message is decoded from the wire.
	 *
	 * @param pos    the position in the buffer where the message should be read from.
	 * @param buffer the buffer to read the message from
	 * @return the read message
	 */
	@Override
	public String decodeFromWire(int pos, Buffer buffer) {
		var end = pos + 10;
		return buffer.getString(pos, end);
	}
	
	/**
	 * If a message is sent <i>locally</i> across the event bus, this method is called to transform the message from
	 * the sent type S to the received type R
	 *
	 * @param localDate the sent message
	 * @return the transformed message
	 */
	@Override
	public String transform(LocalDate localDate) {
		return localDate.toString();
	}
	
	/**
	 * The codec name. Each codec must have a unique name. This is used to identify a codec when sending a message and
	 * for unregistering codecs.
	 *
	 * @return the name
	 */
	@Override
	public String name() {
		return "localDateCodec";
	}
	
	/**
	 * Used to identify system codecs. Should always return -1 for a user codec.
	 *
	 * @return -1 for a user codec.
	 */
	@Override
	public byte systemCodecID() {
		return -1;
	}
}

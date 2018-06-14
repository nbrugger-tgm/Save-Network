package com.niton.tele.network.packs;

import java.io.IOException;

public class MainSocketPacket extends Package<String> {
	private static final long serialVersionUID = -8047865513540715593L;

	public MainSocketPacket(String tolken) throws IOException {
		super("", tolken, false);

	}

	@Override
	public String getName() {
		return "Main_Socket";
	}

	/**
	 * @see com.niton.tele.network.packs.Package#useSeperateSocket()
	 */
	@Override
	public boolean useSeperateSocket() {
		return false;
	}

}

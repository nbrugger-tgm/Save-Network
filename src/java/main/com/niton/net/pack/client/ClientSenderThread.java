package com.niton.net.pack.client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;

import javax.crypto.SealedObject;

import com.niton.net.crypto.SimpleAES;
import com.niton.net.pack.packs.MainSocketPacket;
import com.niton.net.pack.packs.Package;
import com.niton.net.pack.requests.Request;
import com.niton.net.pack.requests.SubConnectionRequest;
import com.niton.net.pack.response.Response;

public class ClientSenderThread extends Thread {
	private NetworkClient c;
	private boolean running = true;

	public ClientSenderThread(NetworkClient client) {
		c = client;
		setName("Client Sender");
	}

	/**
	 * @see java.lang.Thread#destroy()
	 */
	@Override
	public void destroy() {
		running = false;
		try {
			this.join();
		} catch (InterruptedException e) {
		}
	}

	/**
	 * @see java.lang.Thread#interrupt()
	 */
	@Override
	public void interrupt() {
		running = false;
		try {
			this.join();
		} catch (InterruptedException e) {
		}
	}

	/**
	 * @return the running
	 */
	public boolean isRunning() {
		return running;
	}

	@SuppressWarnings("unchecked")
	@Override // Hallo Nils I BIMS DA DOBBI
	public void run() {
		while (running) {
			if (c.getSocket() == null || !c.isConnected()) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
				}
				continue;
			}
			synchronized (c.getQuerry()) {
				ArrayList<Package<? extends Serializable>> sent = new ArrayList<>();
				for (Package<? extends Serializable> pack : c.getQuerry())
					try {
						final Socket subChannel;
						if (pack.useSeperateSocket()) {
							subChannel = new Socket();
							SocketAddress adress = c.getAdress();
							subChannel.connect(adress);
						} else
							subChannel = c.getSocket();
						Runnable send = () -> {
							try {
								synchronized (subChannel) {
									ObjectOutputStream oos;
									oos = new ObjectOutputStream(subChannel.getOutputStream());
									oos.writeObject(pack.isEncrypted());
									Object toSend;
									oos.flush();
									if (pack.isEncrypted()) {
										oos.writeObject(c.getTolken());
										toSend = SimpleAES.encryptObject(pack, c.getAesKey());
									} else
										toSend = pack;
									if (pack instanceof Request)
										if (!pack.useSeperateSocket()) {
											c.getInputListener().notifyNextResponse((Request) pack);
										}
									oos.writeObject(toSend);
									oos.flush();
									if (pack instanceof Request) {
										if (!pack.useSeperateSocket()) {
											((Request) pack).await();
										} else {
											Request req = (Request) pack;
											ObjectInputStream ois = new ObjectInputStream(subChannel.getInputStream());
											boolean crypt = (boolean) ois.readObject();
											Response<? extends Serializable> res;
											if (crypt)
												res = (Response<? extends Serializable>) SimpleAES
														.decryptObject((SealedObject) ois.readObject(), c.getAesKey());
											else
												res = (Response<? extends Serializable>) ois.readObject();
											req.responde(res);
										}
									}
									if (pack.useSeperateSocket() && !(pack instanceof MainSocketPacket) && !(pack instanceof SubConnectionRequest)) {
										subChannel.getOutputStream().close();
										oos.close();
										subChannel.close();
									}
								}
							} catch (Exception e) {
								System.err.println("[Client] Sending Failed : " + e);
							}
						};
//						if (pack.useSeperateSocket())
							new Thread(send, "Sub Socket Sender").start();
//						else
//							send.run();
						sent.add(pack);
						System.gc();
					} catch (Exception e) {
						System.err.println("[Client] Sending Failed : " + e);
					}
				c.getQuerry().removeAll(sent);
			}
		}
	}

	public void stopThread() {
		running = false;
		try {
			this.join();
		} catch (InterruptedException e) {
		}
	}
}

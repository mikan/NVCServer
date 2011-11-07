package jp.ac.jaist.skdlab.nvcsys;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * The non-verbal communication support system - Server program
 * 
 * @author Yutaka Kato
 * @version 0.2.0
 */
public class NVCServer {

	public static final String VERSION = "0.2.0";
	private static int port = 30001;
	private static volatile int nextID = 1;
	private static NVCServer instance = null;
	private ServerSocket serverSocket = null;
	private List<Discussion> discussionList = null;
	private List<NVCClientUser> userList = null;
	
	public static void main(String[] args) {
		
		NVCServer nvcServer = NVCServer.getInstance();
		nvcServer.start();
	}
	
	/**
	 * Singleton constructor
	 */
	private NVCServer() {
		discussionList = new ArrayList<Discussion>();
		userList = new ArrayList<NVCClientUser>();
		System.out.println("Non-verbal communication support system - " +
				"Server program, Version " + VERSION);
	}
	
	public static NVCServer getInstance() {
		if (instance == null) {
			instance = new NVCServer();
		}
		return instance;
	}
	
	public void start() {
		try {
			serverSocket = new ServerSocket(port);
			System.out.println("Server startes at port " + port);
						
			while (!serverSocket.isClosed()) {
				// Wait for client's connection
				Socket clientSocket = serverSocket.accept();
				NVCClientUser user = new NVCClientUser(clientSocket, nextID++);
				addUser(user);
			}
		} catch (BindException e) {
			System.err.println("Bind error by port " + port);
		} catch (IOException e) {
			System.err.println("Server socket IO error");
		}
	}
	
	public void addDiscussion(Discussion discussion) {
		if (discussionList.contains(discussion)) {
			return;
		}
		discussionList.add(discussion);
		System.out.println("Discussion added: " + discussion.getTitle());
		
		// Notify all users
		for (NVCClientUser u : userList) {
			u.reachedMessage("GETD", "");
		}
	}
	
	public Discussion getDiscussion(String title) {
		for (Discussion d : discussionList) {
			if (d.getTitle().equals(title)) {
				return d;
			}
		}
		return null;
	}
	
	public List<Discussion> getDiscussionList() {
		return discussionList;
	}
	
	public void removeDiscussion(Discussion discussion) {
		discussionList.remove(discussion);
		System.out.println("Discussion removed:" + discussion.getTitle());
		
		// Notify all users
		for (NVCClientUser u : userList) {
			u.reachedMessage("GETD", "");
		}
	}
	
	public void clearDiscussion() {
		
		discussionList.clear();
		for (NVCClientUser u : userList) {
			u.reachedMessage("GETD", "");
		}
	}
	
	public void addUser(NVCClientUser user) {
		if (userList.contains(user)) {
			// Already added
			return;
		}
		userList.add(user);
		System.out.println("User added");
	}
	
	public NVCClientUser getUser(String name) {
		for (NVCClientUser u : userList) {
			if (u.getName().equals(name)) {
				return u;
			}
		}
		return null;
	}
	
	public List<NVCClientUser> getUserList() {
		return userList;
	}
	
	public void removeUser(NVCClientUser user) {
		userList.remove(user);
		System.out.println("User removed");
		
		for (Discussion d : discussionList) {
			if (d.containsUser(user)) {
				d.removeUser(user);
			}
		}
	}
	
	public void clearUser() {
		userList.clear();
	}
	
	public void close() throws IOException {
		serverSocket.close();
	}
}
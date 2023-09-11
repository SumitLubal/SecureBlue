package util;

import java.io.IOException;
import java.util.Scanner;

public class RegisterWorkspaceEvent extends Thread{
	volatile private boolean isWorkspaceLocked;
	public void run(){
		isWorkspaceLocked = false;
		try {
			Process eventLockCommand = new ProcessBuilder(Worker.commandLockStatus).start();
			Scanner sc = new Scanner(eventLockCommand.getInputStream());
			while(sc.hasNext()){
				if(sc.nextLine().equalsIgnoreCase("locked")){
					System.out.println("System locked!");
					setOrGetValue(true, true);
				}else if(sc.nextLine().equalsIgnoreCase("unlcoked")){
					System.out.println("System unlocked!");
					setOrGetValue(false, true);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	synchronized public boolean setOrGetValue(boolean value,boolean isSet){
		if(isSet){
			isWorkspaceLocked = value;
		}
		return isWorkspaceLocked;
	}
}

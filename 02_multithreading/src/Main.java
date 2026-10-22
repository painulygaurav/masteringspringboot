public class Main{
    public static void main(String[] args) {
        MyThread t = new MyThread();
        t.start();
        int i = 1;
        while(true){
            System.out.println(i + " World");
            i++;
        }
    }
}

class MyThread extends Thread {
    public void run() {
        int i = 1;
        while(true) {
            System.out.println(i + " Hello");
            i++;
        }
    }
}


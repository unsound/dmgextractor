import java.io.*;

public class BuildEnumerator {
    public static void main(String[] args) throws IOException {
	RandomAccessFile raf = new RandomAccessFile(args[0], "rw");
	String line1 = raf.readLine();
	String line2 = raf.readLine();
	long currentBuild = Long.parseLong(raf.readLine());
	String line4 = raf.readLine();
	String line5 = raf.readLine();
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	PrintStream ps = new PrintStream(baos);
	ps.println(line1);
	ps.println(line2);
	ps.println(++currentBuild + "");
	System.err.println("Current build number: " + currentBuild);
	ps.println(line4);
	ps.println(line5);
	ps.flush();
	byte[] newBytes = baos.toByteArray();
	raf.setLength(newBytes.length);
	raf.seek(0);
	raf.write(newBytes);
	raf.close();
    }
}

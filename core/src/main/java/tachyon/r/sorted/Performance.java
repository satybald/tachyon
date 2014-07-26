package tachyon.r.sorted;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import tachyon.Constants;
import tachyon.TachyonURI;
import tachyon.thrift.TachyonException;

public class Performance {
  private static Logger LOG = Logger.getLogger(Constants.LOGGER_TYPE);

  private static TachyonURI STORE_ADDRESS = null;
  private static ClientStore STORE = null;
  private static List<String> KEYS = new ArrayList<String>();

  public static void main(String[] args) throws IOException, TachyonException, TException {
    // if (args.length != 1) {
    // System.out.println("java -cp target/tachyon-" + Version.VERSION
    // + "-jar-with-dependencies.jar tachyon.r.sorted.Performance <StoreAddress>");
    // System.exit(-1);
    // }

    // STORE_ADDRESS = new TachyonURI(args[0]);
    STORE_ADDRESS = new TachyonURI("tachyon://localhost:19998/perf/16");
    STORE = ClientStore.createStore(STORE_ADDRESS);

    STORE.createPartition(0);
    STORE.put(0, "spark".getBytes(), "5".getBytes());
    STORE.put(0, "the".getBytes(), "3".getBytes());
    STORE.closePartition(0);

    KEYS.add("the");
    KEYS.add("Apache");
    KEYS.add("any");
    KEYS.add("spark");
    KEYS.add("spark");

    int tests = 10000;
    int have = 0;
    long startTimeMs = System.currentTimeMillis();
    for (int k = 0; k < tests; k ++) {
      String key = KEYS.get(k % 5);
      byte[] result = STORE.get(key.getBytes());
      if (result == null) {
        // System.out.println("Key " + key + " does not exist in the store.");
      } else {
        have ++;
        // System.out.println("(" + key + "," + new String(result, "UTF-8") + ")");
      }
    }
    long endTimeMs = System.currentTimeMillis();

    System.out.println("Total time MS: " + (endTimeMs - startTimeMs) + " " + have);
    System.out.println("Average time MS: " + (endTimeMs - startTimeMs) * 1.0 / tests);
    System.exit(0);
  }
}
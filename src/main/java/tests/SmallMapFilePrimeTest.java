package tests;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import BigT.Map;
import bufmgr.*;
import diskmgr.Page;
import global.*;
import heap.HFBufMgrException;
import heap.InvalidSlotNumberException;
import heap.InvalidTupleSizeException;
import storage.SmallMap;
import storage.SmallMapPage;
import storage.Stream;
import storage.SmallMapFile;

class SmallMapFilePrimeDriver extends TestDriver implements GlobalConst {
    int numRec = 200;

    public void setNumRec(Integer numRec) {
        this.numRec = numRec;
        randoms = new Integer[numRec];
    }

    public Integer[] randoms = new Integer[numRec];

    public SmallMapFilePrimeDriver() {
        super("Small Map File Prime Test");
    }

    protected String testName() {
        return "Small Map File Prime";
    }

    protected boolean test1() {
        System.out.println ("\n  Test 1: Insert and scan fixed-size records\n");
        Random rand = new Random();

        for (int i = 0; i < numRec; i++) {
            randoms[i] = rand.nextInt(5000);
        }

        MID rid = new MID();
        SmallMapFile f = null;

        System.out.println ("  - Create a heap file\n");
        try {
            f = new SmallMapFile("file_1", 1, null, MAXROWLABELSIZE);
        } catch (Exception e) {
            System.err.println ("*** Could not create heap file\n");
            e.printStackTrace();
            return false;
        }

        assert SystemDefs.JavabaseBM.getNumUnpinnedBuffers() == SystemDefs.JavabaseBM.getNumBuffers() : "*** The heap-file scan has left pinned pages " + SystemDefs.JavabaseBM.getNumUnpinnedBuffers() + "/" + SystemDefs.JavabaseBM.getNumBuffers();

        System.out.println ("  - Insert " + numRec + " random records\n");
        for (int i = 0; i < numRec; i++) {
            //fixed length record
            Map m1 = new Map();
            try {
                m1.setRowLabel("row1");
                m1.setColumnLabel("col" + randoms[i]);
                m1.setTimeStamp(randoms[i]);
                m1.setValue(Integer.toString(randoms[i]));

                f.insertMap(m1);

//                m1.print();
            } catch (Exception e) {
                System.err.println ("*** Could not make map");
                e.printStackTrace();
                return false;
            }
        }

        Stream stream = null;
        System.out.println ("  - Verify Sorted Stream\n");

        try {
            stream = f.openSortedStream();
        } catch (Exception e) {
            System.err.println ("*** Error opening scan\n");
            e.printStackTrace();
            return false;
        }

        Map map = new Map();
        int count = 0;
        while (map != null) {
            try {
                if (numRec > 0)
                    assert SystemDefs.JavabaseBM.getNumUnpinnedBuffers() != SystemDefs.JavabaseBM.getNumBuffers()
                            : "*** The heap-file scan has not pinned any pages";
                else
                    assert SystemDefs.JavabaseBM.getNumUnpinnedBuffers() == SystemDefs.JavabaseBM.getNumBuffers()
                            : "*** The heap-file scan has left pages pinned despite having 0 records";

                map = stream.getNext(rid);
                if (map == null)
                    break;
//                map.print();

                assert map.getRowLabel().equals("row1");
                assert Integer.parseInt(map.getValue()) == randoms[count]
                        : "Expected value " + randoms[count] + ", got " + Integer.parseInt(map.getValue());
                count++;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        try {
            assert f.getRecCnt() == numRec : "*** File reports " + f.getRecCnt() + " records, not " + numRec;
        } catch (Exception e) {
            System.err.println ("*** Could not invoke getRecCnt on file\n");
            e.printStackTrace();
            return false;
        }

        assert count == numRec : "Returned records from stream doesnt match insert count!";
        assert SystemDefs.JavabaseBM.getNumUnpinnedBuffers() == SystemDefs.JavabaseBM.getNumBuffers() : "*** The heap-file scan has left pinned pages";

        System.out.println ("  Test 1 completed successfully.\n");
        return true;
    }

    protected boolean test2 () {
        System.out.println ("\n  Test 2: Delete fixed-size records\n");
        Stream stream = null;
        MID rid = new MID();
        SmallMapFile f = null;

        System.out.println ("  - Open the same heap file as test 1\n");
        try {
            f = new SmallMapFile("file_1", 1, null, MAXROWLABELSIZE);
        } catch (Exception e) {
            System.err.println ("*** Could not create heap file\n");
            e.printStackTrace();
            return false;
        }

        Map map = null;
        System.out.println ("  - Delete half the records\n");
        try {
            stream = f.openSortedStream();
            map = stream.getNext(rid);
        }
        catch (Exception e) {
            System.err.println ("*** Error opening scan\n");
            e.printStackTrace();
            return false;
        }


        int count = 0;
        while (map != null) {
            try {
                count++;

                // Basically deleting all even records :D
                if (count % 2 != 0) {
                    f.deleteMap(rid);
                }

                map = stream.getNext(rid);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        try {
            stream.closestream();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        assert count == numRec : "*** Record count before deletion does not match!!! Found " + count + " records!";
        assert SystemDefs.JavabaseBM.getNumUnpinnedBuffers() == SystemDefs.JavabaseBM.getNumBuffers() : "*** The heap-file scan has left pinned pages";

        System.out.println ("  - Scan the remaining records\n");
        try {
            stream = f.openSortedStream();
            map = stream.getNext(rid);
        }
        catch (Exception e) {
            System.err.println ("*** Error opening scan\n");
            e.printStackTrace();
            return false;
        }

        count = 1;
        int numRecordsAfterDel = 0;
        while (map != null) {
            try {

                assert map.getRowLabel().equals("row1")
                        : "Got row label " + map.getRowLabel() + " but expected row1";
                assert map.getColumnLabel().equals("col" + randoms[count])
                        : "Got row label " + map.getColumnLabel() + " but expected col" + randoms[count];
                assert map.getTimeStamp() == randoms[count]
                        : "Got row label " + map.getTimeStamp() + " but expected " + randoms[count];
                assert Integer.parseInt(map.getValue()) == randoms[count]
                        : "Got value " + map.getValue() + " but expected " + randoms[count];
//                map.print();
                count += 2;
                numRecordsAfterDel++;
                map = stream.getNext(rid);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        try {
            stream.closestream();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        assert numRecordsAfterDel == numRec / 2
                : "*** Record count before deletion does not match!!! Iterated over " + numRecordsAfterDel  + " records!";
        try {
            assert f.getMapCnt() == numRec / 2
                    : "*** Record count before deletion does not match!!! File reports " + f.getMapCnt()  + " records!";
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        assert SystemDefs.JavabaseBM.getNumUnpinnedBuffers() == SystemDefs.JavabaseBM.getNumBuffers() : "*** The heap-file scan has left pinned pages";

        System.out.println ("  Test 2 completed successfully.\n");
        return true;
    }

    protected boolean test3 () {

        System.out.println ("\n  Test 3: Insert and read multiple primary records\n");
        Stream stream = null;
        MID rid = new MID();
        SmallMapFile f = null;
        HashMap<String, List<Integer>> groups = new HashMap<>();

        System.out.println ("  - Open the same heap file as tests 1 and 2\n");
        try {
            f = new SmallMapFile("file_1", 1, null, MAXROWLABELSIZE);
        } catch (Exception e) {
            System.err.println ("*** Could not create heap file\n");
            e.printStackTrace();
            return false;
        }

        for (int i = 0; i < numRec; i += 2) {
            //fixed length record
            Map m1 = new Map();
            try {
                // Create 9 different primaries (row1 already exists)
                String row = "row" + Integer.toString(randoms[i] % 50 * 2);
                m1.setRowLabel(row);
                m1.setColumnLabel("col" + randoms[i]);
                m1.setTimeStamp(randoms[i]);
                m1.setValue(Integer.toString(randoms[i]));

                if (!groups.containsKey(row)) {
                    groups.put(row, new ArrayList<>());
                }
                groups.get(row).add(randoms[i]);

                f.insertMap(m1);

//                m1.print();
            } catch (Exception e) {
                System.err.println ("*** Could not make map");
                e.printStackTrace();
                return false;
            }
        }

        try {
            assert f.getMapCnt() == numRec : "*** Record count after insertion does not match!!! File reports " + f.getMapCnt()  + " records!";
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        assert SystemDefs.JavabaseBM.getNumUnpinnedBuffers() == SystemDefs.JavabaseBM.getNumBuffers() : "*** The heap-file scan has left pinned pages";

        System.out.println ("  - Verify Sorted Stream\n");

        try {
            stream = f.openSortedStream();
        } catch (Exception e) {
            System.err.println ("*** Error opening scan\n");
            e.printStackTrace();
            return false;
        }

        Map map;
        try {
            map = stream.getNext(rid);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        int count = 0;
        while (map != null) {
            try {
                assert SystemDefs.JavabaseBM.getNumUnpinnedBuffers() != SystemDefs.JavabaseBM.getNumBuffers()
                        : "*** The heap-file scan has not pinned any pages";

                if (map.getRowLabel().equals("row1")) {
                    for (int i = 0; i < numRec / 2; i++) {
                        assert Integer.parseInt(map.getValue()) == randoms[i * 2 + 1]
                                : "Expected value " + randoms[i * 2 + 1] + ", got " + Integer.parseInt(map.getValue());
                        map = stream.getNext(rid);
                        count++;
                    }
                } else {
                    String primary = map.getRowLabel();
                    List<Integer> groupSorted = groups.get(primary);
                    for (Integer integer : groupSorted) {
                        assert map.getRowLabel().equals(primary)
                                : "Expected Row label " + primary + ", got " + map.getRowLabel();
                        assert Integer.parseInt(map.getValue()) == integer
                                : "Expected value " + integer + ", got value " + Integer.parseInt(map.getValue());

                        map = stream.getNext(rid);
                        count++;
                    }
                    groups.remove(primary);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        assert count == numRec : "*** Record count after insertion does not match!!! Iterated over " + count  + " records!";

        try {
            stream.closestream();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        assert SystemDefs.JavabaseBM.getNumUnpinnedBuffers() == SystemDefs.JavabaseBM.getNumBuffers() : "*** The heap-file scan has left pinned pages";

        System.out.println ("  Test 3 completed successfully.\n");
        return true;
    }

    protected boolean test4 () {
        System.out.println ("\n  Test 4: Delete all records\n");
        Stream stream = null;
        MID rid = new MID();
        SmallMapFile f = null;

        System.out.println ("  - Open the same heap file as tests 1 and 2\n");
        try {
            f = new SmallMapFile("file_1", 1, null, MAXROWLABELSIZE);
        } catch (Exception e) {
            System.err.println ("*** Could not create heap file\n");
            e.printStackTrace();
            return false;
        }

        Map map = null;
        System.out.println ("  - Delete all the records\n");
        try {
            stream = f.openSortedStream();
            map = stream.getNext(rid);
        }
        catch (Exception e) {
            System.err.println ("*** Error opening scan\n");
            e.printStackTrace();
            return false;
        }

        int count = 0;
        while (map != null) {
            try {
                f.deleteMap(rid);
                map = stream.getNext(rid);
                count++;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        System.out.println ("  - Verify record count\n");
        assert count == numRec
                : "Iterated over " + count + " records, but expected " + numRec + " records";

        try {
            assert f.getMapCnt() == 0 :
                    "Expected 0 records in file, got " + f.getMapCnt() + " records remaining";
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        try {
            stream.closestream();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        assert SystemDefs.JavabaseBM.getNumUnpinnedBuffers() == SystemDefs.JavabaseBM.getNumBuffers() : "*** The heap-file scan has left pinned pages";
        return true;
    }

    protected boolean test5 () {
        System.out.println ("\n  Test 5: Re-Insert and scan fixed-size records\n");

        Stream stream = null;
        MID rid = new MID();
        SmallMapFile f = null;
        HashMap<String, List<Integer>> groups = new HashMap<>();

        System.out.println ("  - Open the same heap file as tests 1 and 2\n");
        try {
            f = new SmallMapFile("file_1", 1, null, MAXROWLABELSIZE);
        } catch (Exception e) {
            System.err.println ("*** Could not create heap file\n");
            e.printStackTrace();
            return false;
        }

        assert SystemDefs.JavabaseBM.getNumUnpinnedBuffers() == SystemDefs.JavabaseBM.getNumBuffers()
                : "*** The heap-file scan has left pinned pages " + SystemDefs.JavabaseBM.getNumUnpinnedBuffers() + "/" + SystemDefs.JavabaseBM.getNumBuffers();

        System.out.println ("  - Insert " + numRec + " random records\n");
        for (int i = 0; i < numRec; i++) {
            //fixed length record
            Map m1 = new Map();
            try {
                String row = "row" + Integer.toString(randoms[i] % 50 * 2);
                m1.setRowLabel(row);
                m1.setColumnLabel("col" + randoms[i]);
                m1.setTimeStamp(randoms[i]);
                m1.setValue(Integer.toString(randoms[i]));

                if (!groups.containsKey(row)) {
                    groups.put(row, new ArrayList<>());
                }
                groups.get(row).add(randoms[i]);

                f.insertMap(m1);

//                m1.print();
            } catch (Exception e) {
                System.err.println ("*** Could not make map");
                e.printStackTrace();
                return false;
            }
        }

        System.out.println ("  - Verify Sorted Stream\n");

        try {
            stream = f.openSortedStream();
        } catch (Exception e) {
            System.err.println ("*** Error opening scan\n");
            e.printStackTrace();
            return false;
        }

        Map map = null;
        try {
            map = stream.getNext(rid);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        int count = 0;
        while (map != null) {
            try {
                String primary = map.getRowLabel();
                List<Integer> groupSorted = groups.get(primary);
                for (Integer integer : groupSorted) {
                    assert map.getRowLabel().equals(primary)
                            : "Expected Row label " + primary + ", got " + map.getRowLabel();
                    assert Integer.parseInt(map.getValue()) == integer
                            : "Expected value " + integer + ", got value " + Integer.parseInt(map.getValue());

//                    map.print();
                    map = stream.getNext(rid);
                    count++;
                }
                groups.remove(primary);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        try {
            assert f.getRecCnt() == numRec : "*** File reports " + f.getRecCnt() + " records, not " + numRec;
        } catch (Exception e) {
            System.err.println ("*** Could not invoke getRecCnt on file\n");
            e.printStackTrace();
            return false;
        }

        assert count == numRec : "Returned records from stream doesnt match insert count!";
        assert SystemDefs.JavabaseBM.getNumUnpinnedBuffers() == SystemDefs.JavabaseBM.getNumBuffers() : "*** The heap-file scan has left pinned pages";

        System.out.println ("  Test 5 completed successfully.\n");
        return true;
    }
}

public class SmallMapFilePrimeTest {
    public static void main(String[] args) {
        SmallMapFilePrimeDriver fs = new SmallMapFilePrimeDriver();

        Integer[] lengths = {0, 50, 100, 179, 200, 29};
        boolean status = false;
        for (Integer length : lengths) {
            System.out.println("Running SmallMapFile tests with data size: " + length + "\n");
            try {
                fs.setNumRec(length);
                status = fs.runTests();
                if (!status)
                    break;
            } catch (IOException e) {
                System.out.println("Error occured running SmallMapFile tests with data size: " + length);
                e.printStackTrace();
                break;
            }
        }

        if (!status) {
            System.out.println("Error ocurred during test");
            Runtime.getRuntime().exit(1);
        }

        System.out.println("test completed successfully");
        Runtime.getRuntime().exit(0);
    }
}

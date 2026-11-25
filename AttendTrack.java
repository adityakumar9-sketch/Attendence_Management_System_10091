import java.io.*;
import java.time.LocalDate;
import java.util.*;

public class AttendTrack {

    enum Status {
        PRESENT("Present"), ABSENT("Absent"), LATE("Late"), LEAVE("Leave");
        final String label;
        Status(String label) { this.label = label; }

        static Status fromInput(String raw) {
            if (raw == null) return null;
            switch (raw.trim().toUpperCase()) {
                case "P": case "1": case "PRESENT": return PRESENT;
                case "A": case "2": case "ABSENT":  return ABSENT;
                case "L": case "3": case "LATE":    return LATE;
                case "LV": case "4": case "LEAVE":  return LEAVE;
                default: return null;
            }
        }
    }

    static class CourseClass {
        String id, code, name, teacher;
        CourseClass(String id, String code, String name, String teacher) {
            this.id = id; this.code = code; this.name = name; this.teacher = teacher;
        }
    }

    static class Student {
        String id, roll, name, classId, contact;
        Student(String id, String roll, String name, String classId, String contact) {
            this.id = id; this.roll = roll; this.name = name; this.classId = classId; this.contact = contact;
        }
    }

    static class AttendanceEntry {
        Status status; String remark;
        AttendanceEntry(Status status, String remark) {
            this.status = status; this.remark = remark == null ? "" : remark;
        }
    }

    static class Stats {
        int present, absent, late, leave;
        int totalMarked() { return present + absent + late + leave; }
        Integer percentage() {
            int total = totalMarked();
            return total == 0 ? null : (int) Math.round(((present + late) * 100.0) / total);
        }
    }

    static class AttendanceStore {
        private static final String DATA_FILE = "attendtrack_data.db";
        final List<CourseClass> classes = new ArrayList<>();
        final List<Student> students = new ArrayList<>();
        final Map<String, Map<String, AttendanceEntry>> records = new LinkedHashMap<>();

        void loadOrSeed() {
            File file = new File(DATA_FILE);
            if (!file.exists()) {
                seedDemoData();
                saveToFile();
                return;
            }
            classes.clear();
            students.clear();
            records.clear();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] p = line.split("\\|", -1);
                    if ("C".equals(p[0]) && p.length >= 5) {
                        classes.add(new CourseClass(unesc(p[1]), unesc(p[2]), unesc(p[3]), unesc(p[4])));
                    } else if ("S".equals(p[0]) && p.length >= 6) {
                        students.add(new Student(unesc(p[1]), unesc(p[2]), unesc(p[3]), unesc(p[4]), unesc(p[5])));
                    } else if ("R".equals(p[0]) && p.length >= 5) {
                        String key = unesc(p[1]);
                        Status st = Status.fromInput(p[3]);
                        if (st != null) {
                            records.computeIfAbsent(key, k -> new LinkedHashMap<>())
                                    .put(unesc(p[2]), new AttendanceEntry(st, unesc(p[4])));
                        }
                    }
                }
                if (classes.isEmpty()) {
                    seedDemoData();
                    saveToFile();
                }
            } catch (IOException e) {
                System.out.println("Warning: Could not read saved data, loading demo data.");
                seedDemoData();
            }
        }

        void saveToFile() {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE))) {
                for (CourseClass c : classes) {
                    bw.write("C|" + esc(c.id) + "|" + esc(c.code) + "|" + esc(c.name) + "|" + esc(c.teacher));
                    bw.newLine();
                }
                for (Student s : students) {
                    bw.write("S|" + esc(s.id) + "|" + esc(s.roll) + "|" + esc(s.name) + "|" + esc(s.classId) + "|" + esc(s.contact));
                    bw.newLine();
                }
                for (Map.Entry<String, Map<String, AttendanceEntry>> ses : records.entrySet()) {
                    for (Map.Entry<String, AttendanceEntry> ent : ses.getValue().entrySet()) {
                        bw.write("R|" + esc(ses.getKey()) + "|" + esc(ent.getKey()) + "|"
                                + ent.getValue().status.name() + "|" + esc(ent.getValue().remark));
                        bw.newLine();
                    }
                }
            } catch (IOException e) {
                System.out.println("Error saving data: " + e.getMessage());
            }
        }

        void seedDemoData() {
            classes.clear();
            students.clear();
            records.clear();

            String today = LocalDate.now().toString();
            String yesterday = LocalDate.now().minusDays(1).toString();
            String twoDaysAgo = LocalDate.now().minusDays(2).toString();

            classes.add(new CourseClass("c1", "CS-101", "Data Structures", "Prof. R. Sharma"));
            classes.add(new CourseClass("c2", "CS-102", "Database Systems", "Dr. A. Verma"));
            classes.add(new CourseClass("c3", "MATH-201", "Discrete Mathematics", "Prof. S. Iyer"));

            students.add(new Student("s1", "24CS001", "Aarav Sharma", "c1", "aarav@college.edu"));
            students.add(new Student("s2", "24CS002", "Diya Patel", "c1", "diya@college.edu"));
            students.add(new Student("s3", "24CS003", "Kabir Verma", "c1", "kabir@college.edu"));
            students.add(new Student("s4", "24CS004", "Meera Nair", "c1", "meera@college.edu"));
            students.add(new Student("s5", "24CS101", "Vivaan Joshi", "c2", "vivaan@college.edu"));
            students.add(new Student("s6", "24CS102", "Ishita Rao", "c2", "ishita@college.edu"));
            students.add(new Student("s7", "24MA201", "Sneha Reddy", "c3", "sneha@college.edu"));

            Map<String, AttendanceEntry> d1 = new LinkedHashMap<>();
            d1.put("s1", new AttendanceEntry(Status.PRESENT, ""));
            d1.put("s2", new AttendanceEntry(Status.PRESENT, ""));
            d1.put("s3", new AttendanceEntry(Status.ABSENT, "Sick"));
            d1.put("s4", new AttendanceEntry(Status.LATE, "10 min late"));
            records.put(twoDaysAgo + "__c1", d1);

            Map<String, AttendanceEntry> d2 = new LinkedHashMap<>();
            d2.put("s1", new AttendanceEntry(Status.PRESENT, ""));
            d2.put("s2", new AttendanceEntry(Status.LEAVE, "Medical leave"));
            d2.put("s3", new AttendanceEntry(Status.ABSENT, ""));
            d2.put("s4", new AttendanceEntry(Status.PRESENT, ""));
            records.put(yesterday + "__c1", d2);

            Map<String, AttendanceEntry> d3 = new LinkedHashMap<>();
            d3.put("s5", new AttendanceEntry(Status.PRESENT, ""));
            d3.put("s6", new AttendanceEntry(Status.LATE, "Bus delay"));
            records.put(today + "__c2", d3);
        }

        CourseClass findClass(String id) {
            for (CourseClass c : classes) {
                if (c.id.equals(id)) return c;
            }
            return null;
        }

        List<Student> studentsInClass(String classId) {
            List<Student> list = new ArrayList<>();
            for (Student s : students) {
                if (classId == null || classId.isEmpty() || s.classId.equals(classId)) {
                    list.add(s);
                }
            }
            list.sort(Comparator.comparing(s -> s.roll));
            return list;
        }

        Stats computeStats(String studentId, String classId, String fromDate, String toDate) {
            Stats st = new Stats();
            for (Map.Entry<String, Map<String, AttendanceEntry>> e : records.entrySet()) {
                String[] parts = e.getKey().split("__", 2);
                if (parts.length < 2) continue;
                String date = parts[0];
                String cid = parts[1];
                if (classId != null && !classId.isEmpty() && !cid.equals(classId)) continue;
                if (fromDate != null && !fromDate.isEmpty() && date.compareTo(fromDate) < 0) continue;
                if (toDate != null && !toDate.isEmpty() && date.compareTo(toDate) > 0) continue;
                AttendanceEntry entry = e.getValue().get(studentId);
                if (entry == null) continue;
                switch (entry.status) {
                    case PRESENT: st.present++; break;
                    case ABSENT: st.absent++; break;
                    case LATE: st.late++; break;
                    case LEAVE: st.leave++; break;
                }
            }
            return st;
        }

        void exportCsv(String fileName, String classId, String fromDate, String toDate) throws IOException {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
                bw.write("Roll No,Student Name,Class,Present,Late,Leave,Absent,Total Marked,Attendance %");
                bw.newLine();
                for (Student s : studentsInClass(classId)) {
                    CourseClass c = findClass(s.classId);
                    Stats st = computeStats(s.id, s.classId, fromDate, toDate);
                    Integer pct = st.percentage();
                    String clsLabel = c == null ? "Unassigned" : c.code;
                    bw.write(csv(s.roll) + "," + csv(s.name) + "," + csv(clsLabel) + ","
                            + st.present + "," + st.late + "," + st.leave + "," + st.absent + ","
                            + st.totalMarked() + "," + (pct == null ? "" : pct + "%"));
                    bw.newLine();
                }
            }
        }

        private static String esc(String s) {
            return (s == null ? "" : s).replace("\\", "\\\\").replace("|", "\\p").replace("\n", " ");
        }
        private static String unesc(String s) { return s.replace("\\p", "|").replace("\\\\", "\\"); }
        private static String csv(String s) { return "\"" + (s == null ? "" : s).replace("\"", "\"\"") + "\""; }
    }

    private final AttendanceStore store = new AttendanceStore();
    private final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) { new AttendTrack().run(); }

    void run() {
        store.loadOrSeed();
        while (true) {
            System.out.println("\n================ ATTENDTRACK (JAVA) ================");
            System.out.println("1. Mark Daily Attendance\n2. Overview & Low Attendance Alerts (< 75%)\n3. Students & Classes Management\n4. Reports & Export CSV\n5. Reset Demo Data\n0. Exit");
            System.out.print("Choose option: ");
            switch (sc.nextLine().trim()) {
                case "1": markAttendanceFlow(); break;
                case "2": showOverview(); break;
                case "3": manageStudentsAndClasses(); break;
                case "4": showReports(); break;
                case "5":
                    System.out.print("Reset all data to demo? (y/n): ");
                    if (sc.nextLine().trim().equalsIgnoreCase("y")) {
                        store.seedDemoData(); store.saveToFile();
                        System.out.println("Demo data restored.");
                    }
                    break;
                case "0": System.out.println("Goodbye!"); return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private CourseClass pickClass(boolean allowAll) {
        if (store.classes.isEmpty()) {
            System.out.println("No classes found. Please add a class first.");
            return null;
        }
        System.out.println("\nAvailable Classes:");
        if (allowAll) System.out.println("0. All Classes");
        for (int i = 0; i < store.classes.size(); i++) {
            CourseClass c = store.classes.get(i);
            System.out.printf("%d. %s - %s (%s)%n", i + 1, c.code, c.name, c.teacher);
        }
        System.out.print("Select class number: ");
        int idx;
        try {
            idx = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            return null;
        }
        if (allowAll && idx == 0) return new CourseClass("", "ALL", "All Classes", "");
        if (idx < 1 || idx > store.classes.size()) return null;
        return store.classes.get(idx - 1);
    }

    private String promptDate(String label, String defaultVal) {
        System.out.print(label + " [" + defaultVal + "]: ");
        String in = sc.nextLine().trim();
        return in.isEmpty() ? defaultVal : in;
    }

    private void markAttendanceFlow() {
        CourseClass cls = pickClass(false);
        if (cls == null) return;
        String date = promptDate("Enter date (YYYY-MM-DD)", LocalDate.now().toString());
        List<Student> list = store.studentsInClass(cls.id);
        if (list.isEmpty()) {
            System.out.println("No students enrolled in " + cls.code);
            return;
        }

        String key = date + "__" + cls.id;
        Map<String, AttendanceEntry> draft = new LinkedHashMap<>();
        if (store.records.containsKey(key)) {
            for (Map.Entry<String, AttendanceEntry> e : store.records.get(key).entrySet()) {
                draft.put(e.getKey(), new AttendanceEntry(e.getValue().status, e.getValue().remark));
            }
        }

        System.out.println("\nMode: (1) Mark one-by-one  (2) Mark ALL Present  (3) Mark ALL Absent  (4) Clear session");
        System.out.print("Choice [1]: ");
        String mode = sc.nextLine().trim();
        if ("2".equals(mode)) {
            for (Student s : list) draft.put(s.id, new AttendanceEntry(Status.PRESENT, ""));
        } else if ("3".equals(mode)) {
            for (Student s : list) draft.put(s.id, new AttendanceEntry(Status.ABSENT, ""));
        } else if ("4".equals(mode)) {
            draft.clear();
        } else {
            System.out.println("Enter status: P=Present, A=Absent, L=Late, LV=Leave, Enter=keep current");
            for (Student s : list) {
                AttendanceEntry cur = draft.get(s.id);
                String curLabel = cur == null ? "Unmarked" : cur.status.label;
                System.out.printf("%s - %s [%s]: ", s.roll, s.name, curLabel);
                String in = sc.nextLine().trim();
                if (!in.isEmpty()) {
                    Status st = Status.fromInput(in);
                    if (st == null) {
                        System.out.println("  Invalid status, keeping " + curLabel);
                    } else {
                        System.out.print("  Remark (optional): ");
                        String rem = sc.nextLine().trim();
                        draft.put(s.id, new AttendanceEntry(st, rem));
                    }
                }
            }
        }

        if (draft.isEmpty()) store.records.remove(key);
        else store.records.put(key, draft);
        store.saveToFile();
        printSessionSummary(list, draft);
        System.out.println("Attendance saved for " + cls.code + " on " + date + ".");
    }

    private void printSessionSummary(List<Student> list, Map<String, AttendanceEntry> draft) {
        int p = 0, a = 0, l = 0, lv = 0;
        for (Student s : list) {
            AttendanceEntry e = draft.get(s.id);
            if (e == null) continue;
            switch (e.status) {
                case PRESENT: p++; break;
                case ABSENT: a++; break;
                case LATE: l++; break;
                case LEAVE: lv++; break;
            }
        }
        int marked = p + a + l + lv;
        int rate = marked == 0 ? 0 : (int) Math.round(((p + l) * 100.0) / marked);
        System.out.printf("Summary -> Total: %d | Present: %d | Absent: %d | Late: %d | Leave: %d | Rate: %d%%%n",
                list.size(), p, a, l, lv, rate);
    }

    private void showOverview() {
        System.out.printf("%nEnrolled Students: %d | Classes: %d | Recorded Sessions: %d%n",
                store.students.size(), store.classes.size(), store.records.size());
        System.out.println("\n--- Low Attendance Alerts (< 75%) ---");
        boolean anyLow = false;
        for (Student s : store.students) {
            Stats st = store.computeStats(s.id, s.classId, "", "");
            Integer pct = st.percentage();
            if (pct != null && pct < 75) {
                CourseClass c = store.findClass(s.classId);
                System.out.printf("  ! %s (%s) [%s]: %d%% (%d/%d)%n",
                        s.name, s.roll, c == null ? "-" : c.code, pct, st.present + st.late, st.totalMarked());
                anyLow = true;
            }
        }
        if (!anyLow) System.out.println("  All students are at or above 75%.");

        System.out.println("\n--- Recent Sessions ---");
        List<String> keys = new ArrayList<>(store.records.keySet());
        keys.sort(Collections.reverseOrder());
        int limit = Math.min(8, keys.size());
        for (int i = 0; i < limit; i++) {
            String[] p = keys.get(i).split("__", 2);
            CourseClass c = store.findClass(p[1]);
            Map<String, AttendanceEntry> ses = store.records.get(keys.get(i));
            long attended = ses.values().stream().filter(v -> v.status == Status.PRESENT || v.status == Status.LATE).count();
            System.out.printf("  %s | %-8s | %d/%d attended%n", p[0], c == null ? p[1] : c.code, attended, ses.size());
        }
    }

    private void manageStudentsAndClasses() {
        System.out.println("\n1. List Students  2. Add Student  3. Delete Student  4. Add Class  5. Delete Class");
        System.out.print("Choice: ");
        String ch = sc.nextLine().trim();
        if ("1".equals(ch)) {
            for (Student s : store.studentsInClass("")) {
                CourseClass c = store.findClass(s.classId);
                System.out.printf("  %-10s | %-18s | %-8s | %s%n", s.roll, s.name, c == null ? "-" : c.code, s.contact);
            }
        } else if ("2".equals(ch)) {
            CourseClass cls = pickClass(false);
            if (cls == null) return;
            System.out.print("Roll No: "); String roll = sc.nextLine().trim();
            System.out.print("Full Name: "); String name = sc.nextLine().trim();
            System.out.print("Contact (optional): "); String contact = sc.nextLine().trim();
            if (roll.isEmpty() || name.isEmpty()) { System.out.println("Roll & Name required."); return; }
            store.students.add(new Student("s" + System.currentTimeMillis(), roll, name, cls.id, contact));
            store.saveToFile();
            System.out.println("Student added.");
        } else if ("3".equals(ch)) {
            System.out.print("Enter Roll No to delete: ");
            String roll = sc.nextLine().trim();
            boolean removed = store.students.removeIf(s -> s.roll.equalsIgnoreCase(roll));
            store.saveToFile();
            System.out.println(removed ? "Student deleted." : "Roll No not found.");
        } else if ("4".equals(ch)) {
            System.out.print("Class Code (e.g. CS-301): "); String code = sc.nextLine().trim();
            System.out.print("Class Name: "); String name = sc.nextLine().trim();
            System.out.print("Teacher: "); String teacher = sc.nextLine().trim();
            if (code.isEmpty() || name.isEmpty()) { System.out.println("Code & Name required."); return; }
            store.classes.add(new CourseClass("c" + System.currentTimeMillis(), code, name, teacher));
            store.saveToFile();
            System.out.println("Class added.");
        } else if ("5".equals(ch)) {
            CourseClass cls = pickClass(false);
            if (cls == null) return;
            store.classes.removeIf(c -> c.id.equals(cls.id));
            store.students.removeIf(s -> s.classId.equals(cls.id));
            store.saveToFile();
            System.out.println("Class and its students removed.");
        }
    }

    private void showReports() {
        CourseClass cls = pickClass(true);
        if (cls == null) return;
        String from = promptDate("From Date (YYYY-MM-DD)", LocalDate.now().minusDays(30).toString());
        String to = promptDate("To Date (YYYY-MM-DD)", LocalDate.now().toString());
        System.out.printf("%n%-10s %-18s %-9s %4s %4s %4s %4s %5s %6s%n",
                "Roll", "Name", "Class", "P", "L", "Lv", "A", "Total", "%");
        System.out.println("---------------------------------------------------------------------");
        for (Student s : store.studentsInClass(cls.id)) {
            CourseClass c = store.findClass(s.classId);
            Stats st = store.computeStats(s.id, s.classId, from, to);
            Integer pct = st.percentage();
            System.out.printf("%-10s %-18s %-9s %4d %4d %4d %4d %5d %6s%n",
                    s.roll, s.name, c == null ? "-" : c.code,
                    st.present, st.late, st.leave, st.absent, st.totalMarked(), pct == null ? "-" : pct + "%");
        }
        System.out.print("\nExport to CSV? (y/n): ");
        if (sc.nextLine().trim().equalsIgnoreCase("y")) {
            String out = "attendance_report_" + LocalDate.now() + ".csv";
            try {
                store.exportCsv(out, cls.id, from, to);
                System.out.println("Exported to " + out);
            } catch (IOException e) {
                System.out.println("Export failed: " + e.getMessage());
            }
        }
    }
}

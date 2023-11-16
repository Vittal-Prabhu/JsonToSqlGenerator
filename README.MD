# SQL Query Builder

This Java program generates SQL queries based on a flexible JSON configuration. It supports joining multiple tables, and applying WHERE conditions.

## Prerequisites

- Java Development Kit (JDK) installed
- [org.json library] for JSON processing

## How to Use

1. Clone the repository:

    ```bash
    git clone https://github.com/Vittal-Prabhu/JsonToSqlGenerator.git
    ```

2. Navigate to the project directory:

    ```bash
    cd \app\src\main\java\jsontosql
    ```

3. Run the Java code:


## Example Output

```sql
SELECT * FROM students
JOIN attendance ON students.id = attendance.student_id
JOIN teachers ON attendance.teacherId = teachers.id
WHERE students.status = 'active' AND attendance.avaerage >= '75' AND teacher.name = 'Vittal'

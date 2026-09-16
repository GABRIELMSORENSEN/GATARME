"""Run the recurrence engine tests using only JDK 17+."""
import subprocess
from pathlib import Path
root=Path(__file__).resolve().parents[1]
out=root/".tests"
out.mkdir(exist_ok=True)
subprocess.run(["javac","-d",str(out),str(root/"app/src/main/java/com/gatarme/app/Schedule.java"),str(root/"tests/ScheduleTest.java")],check=True)
subprocess.run(["java","-cp",str(out),"ScheduleTest"],check=True)

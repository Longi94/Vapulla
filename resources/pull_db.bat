adb shell "run-as in.dragonbra.vapulla chmod 666 /data/data/in.dragonbra.vapulla/databases/vapulla.db"
adb exec-out run-as in.dragonbra.vapulla cat databases/vapulla.db > vapulla.db
adb shell "run-as in.dragonbra.vapulla chmod 600 /data/data/in.dragonbra.vapulla/databases/vapulla.db"
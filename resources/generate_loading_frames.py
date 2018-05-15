from collections import deque
import sys

if len(sys.argv) < 2:
    print("Usage: [output folder]")
    exit(0)

template = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="24dp"
    android:height="24dp"
    android:viewportHeight="1000"
    android:viewportWidth="1000">
    <path
        android:name="bottomSheet"
        android:fillColor="#546e7a"
        android:pathData="M 500,1000 666,669 ${bottom1X},${bottomY} ${bottom2X},${bottomY} z" />
    <path
        android:name="middleSheet"
        android:fillColor="#707070"
        android:pathData="M 666,669 ${middle1} ${middle2} 500,1000 Z" />
    <path
        android:name="topSheet"
        android:fillColor="#9e9e9e"
        android:pathData="M335,669 L500,1000 1000,0H669Z" />
</vector>
"""

output_dir = sys.argv[1]

FPS = 60.0

length = 2000.0
bottom_delay = 300.0
bottom_delay_frames = int((bottom_delay / 1000.0) * FPS)

middle1_from = (331, 0)
middle1_to = (406, 150)
middle2_from = (166, 331)
middle2_to = (241, 481)

bottomY_from = 0
bottomY_to = 300
bottom1X_from = 331
bottom1X_to = 481
bottom2X_from = 0
bottom2X_to = 150

middle1 = deque()
middle2 = deque()
bottomY = deque()
bottom1X = deque()
bottom2X = deque()


def bounce(t):
    return t * t * 8.0


def interpolation(t):
    t = t * 1.1226
    if t < 0.3535:
        return bounce(t)
    elif t < 0.7408:
        return bounce(t - 0.54719) + 0.7
    elif t < 0.9644:
        return bounce(t - 0.8526) + 0.9
    else:
        return bounce(t - 1.0435) + 0.95


def inter(a, b, t):
    return a + (b - a) * t


for i in range(int((length / 1000.0) * FPS)):
    t = i * 1.0 / FPS

    if t < 1:
        b = interpolation(t)
        middle1.append((inter(middle1_from[0], middle1_to[0], b), inter(middle1_from[1], middle1_to[1], b)))
        middle2.append((inter(middle2_from[0], middle2_to[0], b), inter(middle2_from[1], middle2_to[1], b)))
        bottomY.append(inter(bottomY_from, bottomY_to, b))
        bottom1X.append(inter(bottom1X_from, bottom1X_to, b))
        bottom2X.append(inter(bottom2X_from, bottom2X_to, b))
    else:
        b = interpolation(t - 1.0)
        middle1.append((inter(middle1_to[0], middle1_from[0], b), inter(middle1_to[1], middle1_from[1], b)))
        middle2.append((inter(middle2_to[0], middle2_from[0], b), inter(middle2_to[1], middle2_from[1], b)))
        bottomY.append(inter(bottomY_to, bottomY_from, b))
        bottom1X.append(inter(bottom1X_to, bottom1X_from, b))
        bottom2X.append(inter(bottom2X_to, bottom2X_from, b))

bottomY.rotate(bottom_delay_frames)
bottom1X.rotate(bottom_delay_frames)
bottom2X.rotate(bottom_delay_frames)

frame_length = round(1000.0 / FPS)
with open(output_dir + "/vapulla_loading.xml", "w") as drawable:
    drawable.write("""<animation-list xmlns:android="http://schemas.android.com/apk/res/android"
    android:oneshot="true">
""")
    for i in range(len(middle1)):
        drawable.write(
            "    <item android:drawable=\"@drawable/vapulla_frame_" + str(i) + "\" android:duration=\"" + str(
                frame_length) + "\" />\n")
        xml = template \
            .replace("${middle1}", str(middle1[i][0]) + "," + str(middle1[i][1])) \
            .replace("${middle2}", str(middle2[i][0]) + "," + str(middle2[i][1])) \
            .replace("${bottomY}", str(bottomY[i])) \
            .replace("${bottom1X}", str(bottom1X[i])) \
            .replace("${bottom2X}", str(bottom2X[i]))

        with open(output_dir + "/vapulla_frame_" + str(i) + ".xml", "w") as f:
            f.write(xml)
    drawable.write("</animation-list>")

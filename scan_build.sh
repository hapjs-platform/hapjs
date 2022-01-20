file="mockup/platform/android/app/build/reports/checkstyle-report.xml"
if [ ! -f "$file" ]; then
	echo "file checkstyle-report.xml not produce"
	exit 1
else 
	grep -c "<error" $file
	if [ $? -eq 0 ]; then 
		echo "scan failed, please download artifacts to see scan result"
		exit 1
	fi
fi
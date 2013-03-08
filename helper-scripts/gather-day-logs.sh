#!/bin/bash
#############################################################################
# Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática  #
# Departamento de Engenharia Electrotécnica e de Computadores               #
# Rua Dr. Roberto Frias, 4200-465 Porto, Portugal                           #
#############################################################################
# Author: Paulo Dias, José Pinto                                            #
#############################################################################
# $Id:: gather-day-logs.sh 9615 2012-12-30 23:08:28Z pdias                $:#
#############################################################################
# This script will prepare the logs from Neptus or collected by Neptus to   #
# be uploaded to the mission repository.                                    #
#############################################################################

PROGNAME=$0
PROGDIRNAME=`dirname $PROGNAME`


if [ -z "$BASH_VERSION" ]; then
    /bin/bash $0
    exit $?
fi

TEST_ZENITY=$(zenity -h 2>&1 1>/dev/null; echo $?)
if [ "$TEST_ZENITY" -ne 0 ]; then
    echo "Please install Zenity before proceding"
    exit $TEST_ZENITY
fi

TEST_7Z=$(7z 2>&1 1>/dev/null; echo $?)
if [ "$TEST_7Z" -ne 0 ]; then
    echo "Please install 7Zip before proceding"
    zenity --title="Please install 7Zip" --error --text="Please install 7Zip before proceding"
    exit $TEST_7Z
fi


START_PWD=$(pwd)
cd $PROGDIRNAME/..
NEPTUS_HOME=$(pwd)
cd $PROGDIRNAME
HOSTNAME=`hostname`
NEPTUSDIR=$(echo $HOSTNAME|tr "[:upper:]" "[:lower:]"|tr " " "_")

export todayFind=$(date +%Y%m%d)0000
export todayDirName=$(date +%Y%m%d)
export dest=to_upload_$todayDirName

STARTTIME=`date +%s`

if [ $(ps aux |grep -c  neptus.jar) -gt 1 ]; then
  echo "Seems that Neptus is open. Close it please..."
  zenity --title="Create package" --error --text="Seems that Neptus is open. Close it please..."
  exit 1
fi

PWD=$(pwd)
to_upload=`zenity --title="Select package destination folder" --file-selection --directory --save --filename=$NEPTUS_HOME/$dest`

if [ $? = 1 ]; then
  exit 1
fi


cd $NEPTUS_HOME

echo "Exporting from $NEPTUS_HOME/log to $to_upload"

(
touch -t $todayFind _start

echo "# Creating a clean folder '"$to_upload"/'"
rm -Rf $to_upload && mkdir $to_upload

if [ $? -ne 0 ]; then
  zenity --title="Create package" --error --text="Delete "$to_upload" please!";
  echo "Delete "$to_upload" please!";
  exit 1;
fi
echo 10

echo "# Moving 'log/downloaded' to '"$to_upload"/'"
echo "# Moving 'log/downloaded' to '"$to_upload"/'"
mv -v $NEPTUS_HOME/log/downloaded $to_upload/
echo "# Deleting LLF and temporary MRA files from 'log/downloaded'"
find $to_upload/downloaded -name *.llf|while read fx; do rm -v "$fx"; done;
find $to_upload/downloaded -name *.mra|while read fx; do rm -v "$fx"; done;
find $to_upload/downloaded -name lsf.index|while read fx; do rm -v "$fx"; done;

echo "# Compressing LSF files"
find $to_upload/downloaded -name *.lsf|while read fx; do gzip -n -9 -v "$fx"; done;

mv $to_upload/downloaded/* $to_upload/ && rmdir $to_upload/downloaded

echo 25
echo "# Preparing Neptus log dir"
mkdir -p $to_upload/$NEPTUSDIR/$todayDirName
mv -v $NEPTUS_HOME/log/* $to_upload/$NEPTUSDIR/$todayDirName
svnversion > $to_upload/$NEPTUSDIR/$todayDirName/svninfo.txt
svn info >> $to_upload/$NEPTUSDIR/$todayDirName/svninfo.txt
svn status >> $to_upload/$NEPTUSDIR/$todayDirName/svninfo.txt
echo 40
echo "# Finding used  mission file..."
find $NEPTUS_HOME/missions/ -type f -name '*.nmisz' -newer _start -exec cp -v {} $to_upload/$NEPTUSDIR/$todayDirName \;
echo 50
cd $to_upload/$NEPTUSDIR/$todayDirName/
echo "# Zipping debug.log*"
zip -rv log-debug.zip debug.log* && rm -v debug.log*
echo 60
echo "# Zipping output/*"
zip -rv output.zip output/* && rm -rvf output/
echo 70

# echo "# Zipping messages/*"
# zip -rv messages.zip messages/* && rm -rvf messages/

echo "# Deleting LLF and temporary MRA files from 'log/messages'"
find messages -name *.llf|while read fx; do rm -v "$fx"; done;
find messages -name *.mra|while read fx; do rm -v "$fx"; done;
find messages -name lsf.index|while read fx; do rm -v "$fx"; done;
echo "# Compressing LSF files"
find messages -name *.lsf|while read fx; do gzip -n -9 -v "$fx"; done;

echo 80
echo "# 7zipping mission_state/*"
7z a -t7z mission_state.7z mission_state/* -mx5  && rm -rvf mission_state/
echo 90

cd $NEPTUS_HOME
rm -v _start
cd $START_PWD

ENDTIME=`date +%s`
TOTALTIME=$(($ENDTIME-$STARTTIME))
echo "#Work done in $TOTALTIME seconds."
echo 100
) | zenity --progress --title="Creating package" --text="Initializing" --pulsate

zenity --notification --text="Check $to_upload/$NEPTUSDIR/$todayDirName and see if something is missing or unneeded."

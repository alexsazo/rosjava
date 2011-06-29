# set EXTRA_CMAKE_FLAGS in the including Makefile in order to add tweaks
CMAKE_FLAGS= -Wdev -DCMAKE_TOOLCHAIN_FILE=`rospack find rosbuild`/rostoolchain.cmake $(EXTRA_CMAKE_FLAGS)

# The all target does the heavy lifting, creating the build directory and
# invoking CMake
all: ant-properties eclipse-project eclipse-classpath msg-deps
	ant

PACKAGE_NAME=$(shell basename $(PWD))

ant-properties:
	rosrun rosjava_bootstrap generate_properties.py $(PACKAGE_NAME) > ros.properties

eclipse-classpath:
	if [ ! -f .classpath ] ; then rosrun rosjava_bootstrap generate_properties.py --eclipse $(PACKAGE_NAME) > .classpath ; touch .classpath-generated; fi

eclipse-project:
	if [ ! -f .project ] ; then cp `rospack find rosjava_bootstrap`/eclipse/eclipse-project-template .project ; touch .project-generated; fi

# msg-deps builds both msgs and srvs
msg-deps:
	rosrun rosjava_bootstrap generate_msg_depends.py $(PACKAGE_NAME)

clean:
	-if [ -f .project-generated ] ; then rm .project .project-generated; fi
	-if [ -f .classpath-generated ] ; then rm .classpath .classpath-generated; fi
	-rm ros.properties
	-ant clean

wipe-msgs:
	rosrun rosjava_bootstrap generate_msg_depends.py --wipe $(PACKAGE_NAME)

# All other targets are just passed through
test: all
	ant test


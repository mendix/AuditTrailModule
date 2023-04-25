#!/bin/sh

echo "Copying the project ..."
rm -rf ./upgraded
mkdir /tmp/upgraded
cp -r ./* /tmp/upgraded
mkdir upgraded
mv /tmp/upgraded .
cd upgraded

echo "Converting the project ..."
mx convert --in-place "./"
mxbuild --target=deploy "Audit trail.mpr"

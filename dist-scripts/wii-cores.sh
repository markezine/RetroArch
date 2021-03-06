#!/bin/sh

make -C ../ -f Makefile.wii.salamander clean || exit 1
make -C ../ -f Makefile.wii clean || exit 1

make -C ../ -f Makefile.wii.salamander || exit 1
make -C ../ -f Makefile.wii.salamander pkg || exit 1

for f in *_wii.a ; do
   name=`echo "$f" | sed 's/\(_libretro\|\)_wii.a$//'`
   whole_archive=
   big_stack=
   if [ $name = "nxengine" ] ; then
      whole_archive="WHOLE_ARCHIVE_LINK=1"
   fi
   if [ $name = "tyrquake" ] ; then
      big_stack="BIG_STACK=1"
   fi
   cp -f "$f" ../libretro_wii.a
   make -C ../ -f Makefile.wii $whole_archive $big_stack -j3 || exit 1
   mv -f ../retroarch_wii.dol ../wii/pkg/$name.dol
   rm -f ../retroarch_wii.dol ../retroarch_wii.elf ../retroarch_wii.elf.map
done

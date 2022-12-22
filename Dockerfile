# Copyright 2022 Aleksei Balan
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM maven:3.8-openjdk-11 AS build
COPY . ./
RUN mvn clean package

FROM debian:11-slim
RUN sed -i -e 's/bullseye main/bullseye main non-free/' /etc/apt/sources.list &&\
  apt-get update &&\
  apt-get install -y libttspico-utils openjdk-11-jre-headless wget bzip2 sgml-base libncurses5 sysv-rc &&\
  wget http://archive.debian.org/debian/pool/main/e/esound/esound-common_0.2.36-3_all.deb &&\
  dpkg -i esound-common_0.2.36-3_all.deb &&\
  wget http://archive.debian.org/debian/pool/main/a/audiofile/libaudiofile0_0.2.6-8_amd64.deb &&\
  dpkg -i libaudiofile0_0.2.6-8_amd64.deb &&\
  wget http://archive.debian.org/debian/pool/main/e/esound/libesd0_0.2.36-3_amd64.deb &&\
  dpkg -i libesd0_0.2.36-3_amd64.deb &&\
  wget http://archive.debian.org/debian/pool/main/s/speech-tools/libestools1.2_1.2.96~beta-2_amd64.deb &&\
  dpkg -i libestools1.2_1.2.96~beta-2_amd64.deb &&\
  wget http://archive.debian.org/debian/pool/main/f/festival/festival_1.96~beta-7_amd64.deb &&\
  ln -s /bin/echo /usr/local/bin/install-info &&\
  dpkg -i festival_1.96~beta-7_amd64.deb &&\
  rm /usr/local/bin/install-info &&\
  apt-get -y install festlex-cmu festlex-poslex festvox-kallpc16k &&\
  rm -rf *.deb &&\
  rm -rf /var/lib/apt/lists/*

RUN mkdir -p /usr/share/festival/voices/ &&\
  wget http://hts.sp.nitech.ac.jp/archives/2.1/festvox_nitech_us_awb_arctic_hts-2.1.tar.bz2 &&\
  wget http://hts.sp.nitech.ac.jp/archives/2.1/festvox_nitech_us_bdl_arctic_hts-2.1.tar.bz2 &&\
  wget http://hts.sp.nitech.ac.jp/archives/2.1/festvox_nitech_us_clb_arctic_hts-2.1.tar.bz2 &&\
  wget http://hts.sp.nitech.ac.jp/archives/2.1/festvox_nitech_us_jmk_arctic_hts-2.1.tar.bz2 &&\
  wget http://hts.sp.nitech.ac.jp/archives/2.1/festvox_nitech_us_rms_arctic_hts-2.1.tar.bz2 &&\
  wget http://hts.sp.nitech.ac.jp/archives/2.1/festvox_nitech_us_slt_arctic_hts-2.1.tar.bz2 &&\
  for z in `ls *.bz2` ; do tar xvf $z ; done &&\
  mv lib/voices/* /usr/share/festival/voices/ &&\
  mv lib/hts.scm /usr/share/festival/ &&\
  rm -rf *.bz2

COPY --from=build /target/festival-api-jar-with-dependencies.jar /festival-api.jar
EXPOSE 59125
CMD ["java", "-jar", "/festival-api.jar"]

# docker run -d --rm --name festival-api -p 59125:59125 festival-api

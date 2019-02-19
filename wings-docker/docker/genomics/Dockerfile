FROM kcapd/wings-base

# Install Genomics Domain Specific Stuff
RUN sed -i 's/debian stretch main/debian stretch main contrib non-free/' /etc/apt/sources.list
RUN cat /etc/apt/sources.list
RUN apt-get update
RUN apt-get -y install libbz2-dev liblzma-dev
RUN apt-get -y install python-dev samtools tophat cufflinks
RUN apt-get -y install python-setuptools
RUN apt-get -y install python-numpy
RUN apt-get -y install libz-dev
RUN pip install RSeQC

# Install R and stuff
RUN apt-get -y install r-base r-base-dev
RUN apt-get -y install libssl-dev
ADD ./docker/genomics/R-install.R /tmp/R-install.R
RUN Rscript /tmp/R-install.R

# Start WINGS
#RUN chmod 755 /setenv.sh 
#CMD /setenv.sh && service tomcat8 start && /bin/bash
#RUN sed -i 's/debian testing main/debian testing main contrib non-free/' /etc/apt/sources.list

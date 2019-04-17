FROM kcapd/wings-base

# Install Genomics Domain Specific Stuff
RUN sed -i 's/debian stretch main/debian stretch main contrib non-free/' /etc/apt/sources.list
RUN apt-get update && apt-get -y install --no-install-recommends \
        libbz2-dev \
        liblzma-dev \
        python-dev \
        samtools \
        tophat \
        cufflinks \
        python-setuptools \
        python-numpy \ 
        libz-dev \
        r-base \
        r-base-dev \
        libssl-dev

RUN pip install RSeQC
ADD wings-docker/docker/genomics/R-install.R /tmp/R-install.R
RUN Rscript /tmp/R-install.R

# Start WINGS
#RUN chmod 755 /setenv.sh 
#CMD /setenv.sh && service tomcat8 start && /bin/bash
#RUN sed -i 's/debian testing main/debian testing main contrib non-free/' /etc/apt/sources.list

@echo killing excel
cd %1

@echo wait excel to be closed
timeout /t 10

@echo Removing ICT file
del ICT.xlsm

@echo Renaming ICT update
ren ICT_update.xlsm ICT.xlsm

@echo launching ICT in excel
start excel "ICT.xlsm"

pause
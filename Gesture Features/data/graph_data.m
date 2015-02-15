cd 'C:\Users\jcher_000\Desktop\Raw Data Cleaned\'
files = dir(pwd);
files = files(3:end);
for a = 1:numel(files)
filename = files(a).name;
fid = fopen(filename);

titles = textscan(fid,'%s$f','delimiter','\n');
t_array = [];
x_array = [];
y_array = [];
z_array = [];
while (~feof(fid))
    cur_line = textscan(fid,'%s$f','delimiter','\n');
    cur_line = cur_line{1}{1};
    [t, cur_line] = strtok(cur_line, ','); %#ok<*STTOK>
    t_array(end+1) = str2double(t(2:end-1));
    [x, cur_line] = strtok(cur_line, ',');
    x_array(end+1) = str2double(x(2:end-1));
    [y, cur_line] = strtok(cur_line, ',');
    y_array(end+1) = str2double(y(2:end-1));
    [z, cur_line] = strtok(cur_line, ',');
    z_array(end+1) = str2double(z(2:end-1));
end

total_time = 0;
prev_time = t_array(1);

time_intervals = [];

for  i = 2:numel(t_array)
  cur_time = t_array(i);
  
  total_time = total_time + (cur_time - prev_time);
  prev_time = cur_time;
  
  if total_time > 1
      time_intervals = [time_intervals cur_time]; %#ok<*AGROW>
      total_time = 0;
  end
end

%loop through time array
fclose('all');

h = figure('units','normalized','outerposition',[0 0 1 1], 'visible', 'off');

plot(t_array, x_array, t_array, y_array, t_array, z_array);
y_line = [min([x_array(:);y_array(:);z_array(:)]), max([x_array(:);y_array(:);z_array(:)])];
hold on
for n = 1:numel(time_intervals)
    x_line = [time_intervals(n) time_intervals(n)];
    
    plot(x_line, y_line, 'k');
end
hold off

title(filename);
xlabel('Time(s)');
ylabel('Acceleration(mG)');
hgexport(gcf, strcat(filename(1:end-4),'.jpg'), hgexport('factorystyle'), 'Format', 'jpeg');
end



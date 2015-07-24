format long;

if ~exist('Filtered Data','dir')
    mkdir('Filtered Data');
end

cd 'Raw Data Cleaned\'
files = dir(pwd);
files = files(3:end);

for a = 1:numel(files)
    filename = files(a).name;
    [name, ext] = strtok(filename,'.');
    if strcmp(ext, '.csv')
        
        fid = fopen(filename);
        
        title = textscan(fid,'%s$f','delimiter','\n');
        for c = 1:5
            [titles(c), title] = strtok(title(1), ',');
            titles{c} = titles{c}{1};
        end
        
        %Definitions
        
        conv = 9.80666/1000; %conversion factor to m/s^2
        
        %titles = textscan(fid,'%s$f','delimiter','\n');
        t_array = [];
        x_accel = [];
        y_accel = [];
        z_accel = [];
        gesture_array = [];
        
        while (~feof(fid))
            cur_line = textscan(fid,'%s$f','delimiter','\n');
            cur_line = cur_line{1}{1};
            if strcmp(cur_line, '')
                break;
            end
            [t, cur_line] = strtok(cur_line, ','); %#ok<*STTOK>
            t = str2num(t);
            t_array(end+1) = t;
            
            [x, cur_line] = strtok(cur_line, ',');
            x = str2num(x)*conv;
            x_accel(end+1) = x;
            
            [y, cur_line] = strtok(cur_line, ',');
            y = str2num(y)*conv;
            y_accel(end+1) = y;
            
            [z, cur_line] = strtok(cur_line, ',');
            z = str2num(z)*conv;
            z_accel(end+1) = z;
            
            [gesture, cur_line] = strtok(cur_line, ',');
            gesture_array{end+1} = gesture;
        end
        fclose('all');
        
        %Stop displaying graphs, return filtered data
        cd ..
        [X, f_x, x_freq_unf, x_freq_filter] = fftf(t_array, x_accel);
        [Y, f_y, y_freq_unf, y_freq_filter] = fftf(t_array, y_accel);
        [Z, f_z, z_freq_unf, z_freq_filter] = fftf(t_array, z_accel);
        
        % figure;
        % % first plot
        % subplot(2,1,1)
        % plot(t_array,x_accel)
        % xlabel('uSec')
        % axis tight
        % title('Original signal')
        % % third plot
        % subplot(2,1,2)
        % plot(t_array,X)
        % xlabel('uSec')
        % axis tight
        cd 'Filtered Data'
        
        %csv_line = zeros(numel(X),5);
        for d = 1:numel(titles)
            csv_line{1,d} = titles{d};
        end
        for b = 1:numel(X)
            csv_line{b+1, 1} = t_array(b);
            csv_line{b+1, 2} = X(b);
            csv_line{b+1, 3} = Y(b);
            csv_line{b+1, 4} = Z(b);
            csv_line{b+1, 5} = gesture_array{b};
        end
        fid_filter = fopen(filename, 'w');
        fprintf(fid_filter,'%s, %s, %s, %s, %s\n',csv_line{1,:});
        
        [row, ~] = size(csv_line);
        for e=2:row
            fprintf(fid_filter,'%f, %f, %f, %f, %s\n',csv_line{e,:});
        end
        csv_line(:,:) = [];
        %csvwrite(filename, csv_line);
        %create matrix to write
        
        %cd 'Raw Data Cleaned\'
        %save filtered data to new folder
        cd ..
        cd 'Raw Data Cleaned\'
    end
end

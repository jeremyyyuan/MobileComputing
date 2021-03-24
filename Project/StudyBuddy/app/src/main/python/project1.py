from sklearn.cluster import SpectralClustering
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
import pandas as pd
import re
import numpy as np
import matplotlib.pyplot as plt
from scipy.signal import find_peaks
from sklearn import tree
import os

from sklearn.metrics import confusion_matrix

def extract_value_output_magnitude(file_data):
    mean = file_data.mean(0)
    sigma = np.std(file_data, axis =0)
    #print(mean.shape)
    #need tuple of mean and sigma
    #print(sigma[1])
    sigma_magnitude = np.sqrt(sigma[1]**2 +sigma[2]**2 +sigma[3]**2)
    mean_magnitude = np.sqrt(mean[1]**2 +mean[2]**2 +mean[3]**2)
    
    x_mean = mean[1]
    y_mean = mean[2]
    z_mean = mean[3]
    x_sigma = sigma[1]
    y_sigma = sigma[2]
    z_sigma = sigma[2]
    
    select = (x_mean, y_mean, z_mean, x_sigma, y_sigma, z_sigma)
    
    return select


def activity_feature_extaction(model_data):
    new_model_data = {activity: [] for activity in activities}
    for activity in activities:
        #print(model_data[activity])
        # Column Mean in tuple list
        # using list comprehension + sum() + zip()
        res = [sum(ele) / len(model_data[activity]) for ele in zip(*model_data[activity])]
        #print(res)
        #new_mean = model_data[activity].mean(0)
        new_model_data[activity].append(res)
    
    return new_model_data

def get_peak_freq(file_data):
    #print(len(file_data))
    time_interval = len(file_data)
    x = np.linspace(0, time_interval, time_interval)
    #print(len(x))
    #x= np.array(x)
    #print(x)
    x_vals = [item[1] for item in file_data]
    x=x[1:]
    y = np.array([float(item)*9.8 for item in x_vals[1:]])
    #print(y)
    # x-axis
    x_accel = y[:]
    #print(x_accel)
    peaks_x, _ = find_peaks(x_accel, height = 0)
    '''important DONT DELETE
    plt.plot(x, x_accel)
    plt.plot(peaks_x, x_accel[peaks_x], "x")
    plt.plot(np.zeros_like(x_accel), "--", color="gray")
    plt.show()
    print(len(peaks_x))
    '''
    
    x_peak_freq = len(peaks_x)/len(x)
    #print(x_peak_freq)
    
    
    
    #print("y-axis")
    #y-axis
    #print(y)
    y_vals = [item[2] for item in file_data]
    y = np.array([float(item)*9.8 for item in y_vals[1:]])
    y_accel = y[:]
    #print(y_accel)
    peaks_y, _ = find_peaks(y_accel, height = 0)
    
    '''
    plt.plot(x, y_accel)
    plt.plot(peaks_y, y_accel[peaks_y], "x")
    plt.plot(np.zeros_like(y_accel), "--", color="gray")
    plt.show()
    print(len(peaks_y))
    '''
    
    
    y_peak_freq = len(peaks_y)/len(x)
    #print(y_peak_freq)
    
    #print("z-axis")
    #z-axis
    z_vals = [item[3] for item in file_data]
    y = np.array([float(item)*9.8 for item in z_vals[1:]])
    z_accel = y
    #print(z_accel)
    peaks_z, _ = find_peaks(z_accel, height = 0)
    plt.plot(x, z_accel, label='z-accel')
    plt.plot(x, y_accel, label='y-accel')
    plt.plot(x, x_accel, label='x-accel')
    plt.legend()
    plt.plot(peaks_z, z_accel[peaks_z], "x")
    plt.plot(np.zeros_like(z_accel), "--", color="gray")
    plt.show()
    print(len(peaks_z))
    
    
    z_peak_freq = len(peaks_z)/len(x)
    #print(z_peak_freq)
    
    return (x_peak_freq, y_peak_freq, z_peak_freq)
    

def append_activity(list_act, activity):
    if(activity == 'call'):
        list_act.append(1)
    if(activity == 'no_motion'):
        list_act.append(2)
    if(activity == 'pickup'):
        list_act.append(3)
    if(activity == 'table_tap'):
        list_act.append(4)
    if(activity == 'walk'):
        list_act.append(5)

def print_confusion_matrix(activities, clf):
    test_data = []
    
    dir_path = os.path.dirname(__file__)
    proc_dir = os.path.join(dir_path, "test_preprocessed")

    i = 0
    y_true = []
    global y_pred
    y_pred = []
    while(i<2):
        test_data = []
        cur_predict_num = 0
        for activity in activities:
            append_activity(y_true, activity)
            filename = activity + str(i) + '.txt'
            join_filename = os.path.join(proc_dir, str(filename))
            with open(join_filename, 'r') as f:
                file_data = np.genfromtxt(join_filename,delimiter=';',skip_header=1)
                ana_file_data = extract_value_output_magnitude(file_data)
                test_data.append(ana_file_data)
                activity_number = clf.predict(test_data)
                #print(activity_number)
                if(activity_number[cur_predict_num] == 1):
                    activity_name = 'call'
                if(activity_number[cur_predict_num] == 2):
                    activity_name = 'no_motion'
                if(activity_number[cur_predict_num] == 3):
                    activity_name = 'pickup'
                if(activity_number[cur_predict_num] == 4):
                    activity_name = 'table_tap'
                if(activity_number[cur_predict_num] == 5):
                    activity_name = 'walk'
                y_pred.append(activity_number[cur_predict_num])
                cur_predict_num += 1
                print(filename + ': ' + activity_name)
        i = i + 1
    mtx = confusion_matrix(y_true, y_pred, normalize='true')
    print(mtx)

def get_activities(data):
    activity_dict = {"phone_pickups":1, "leave_app":0, "tap_number":2, "swipe_number":2, "notification_number": 0}
    #activity_dict = {"phone_pickups":2, "leave_app":0, "tap_number":0, "swipe_number":0, "notification_number": 0}
    #peak_freq = get_peak_freq(data)
    #print(peak_freq)
    
    return activity_dict
    
def get_distractions(dict):
    a = dict['phone_pickups']
    b = dict['leave_app']
    c = dict['tap_number']
    d = dict['swipe_number']
    e = dict['notification_number']
    total_distractions = 0
    # Picking up the phone to check the time would not count as a distraction.
    if a > 0 and b == 0 and c == 0 and d == 0:
        return 0
    
    # Pickup phone tap or swipe greater than or equal to 2, given phone pickup > 1
    if (a > 0) and (c >= 2 or d >= 2):
        return a
    
    # Leaving the study app to surf other applications
    if b > 0:
        # Number of taps > 1 within a small time frame
        if c > 0:
            total_distractions += c
        # Tap occurs immediately after a notification occurrence
        if c == e:
            total_distractions += e
        # Swipes > 1 would count as distraction
        if d > 0:
            total_distractions += d
        return total_distractions
        
    else:
        return 0

def classifyData(path2):
    
    df = pd.DataFrame(columns = ['X','Y','Z'])
    
    """
    for x in os.listdir(r"./"):
        print("file: {}".format(x))
    os.chdir(r"/train/")
    """
    activities = ['call', 'no_motion', 'pickup', 'table_tap', 'walk']
    model_data = {activity: [] for activity in activities}
    Y_Train = []
    dir = os.path.join(os.path.dirname(__file__), "train")
    print('Training model with training data...')
    for activity in activities:
        act_dir = dir + '/' + activity
        for filename in os.listdir(act_dir):
            path = act_dir + '/' + filename
            file_data = np.genfromtxt(path,delimiter=';',skip_header=1)
            file_data = extract_value_output_magnitude(file_data)
            model_data[activity].append(file_data)
            # Create the proper feature array Y_Train
            if(activity == 'call'):
                Y_Train.append(1)
            if(activity == 'no_motion'):
                Y_Train.append(2)
            if(activity == 'pickup'):
                Y_Train.append(3)
            if(activity == 'table_tap'):
                Y_Train.append(4)
            if(activity == 'walk'):
                Y_Train.append(5)
        #print(str(activity) + " training complete")
    
    X_Train = []
    for activity in activities:
        for x in model_data[activity]:
            X_Train.append(x)
    
    
    clf = tree.DecisionTreeClassifier()
    X_Train = np.array(X_Train)
    #print(X_Train)
    #print(Y_Train)
    clf = clf.fit(X_Train, Y_Train)
    
    '''
    file_name = input('Enter the file name of the data you would like to classify: ')
    
    directory = '/Users/alanzhao/Desktop/Mobile Computing/Project 1/test_preprocessed/' + str(file_name)
    print(directory)
    test_data = []
    if directory.endswith(".txt"):  # You could also add "and i.startswith('f')
        with open(directory, 'r') as f:
            file_data = np.genfromtxt(directory,delimiter=';',skip_header=1)
            #print(file_data)
            ana_file_data = extract_value_output_magnitude(file_data)
            #print(ana_file_data)
            test_data.append(ana_file_data)
    
    prediction_result = clf.predict(test_data)
    if(prediction_result[0] == 1):
        activity_name = 'call'
    if(prediction_result[0] == 2):
        activity_name = 'no_motion'
    if(prediction_result[0] == 3):
        activity_name = 'pickup'
    if(prediction_result[0] == 4):
        activity_name = 'table_tap'
    if(prediction_result[0] == 5):
        activity_name = 'walk'
    
    print('The probable activity in ' + str(file_name) + ' is ' + activity_name + '.')
    '''
    print_confusion_matrix(activities, clf)
    print(os.getcwd())
    
    
    # path2 = './samples/pick_up_put_down/pupd2ice_30hz.txt'
    data = []
    with open(path2) as rf:
        lines = rf.readlines()
        for line in lines:
            line = line.strip()
            parts = re.split(r';' , line)
            data.append((parts[0], parts[1], parts[2], parts[3]))
            print(parts)
    
    
    # 1 determine number of times user picks up the phone
    # 2 determine how many times user tap the phone
    # 3 determine how many times user scrolls the phone
    # 4 determine how many times user swipes the phone
    # data is the filename and what it returns is a dictionary
    activity_dict = get_activities(data)

    #How do I define distraction:
    #   1. More than two taps on the phone
    #   2. tap occurs immediately after notification
    distraction_number = get_distractions(activity_dict)
    print("distraction total: " + str(distraction_number))
    return str(distraction_number)

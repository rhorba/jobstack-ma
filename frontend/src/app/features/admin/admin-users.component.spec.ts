import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AdminUsersComponent } from './admin-users.component';

describe('AdminUsersComponent', () => {
  let fixture: ComponentFixture<AdminUsersComponent>;
  let component: AdminUsersComponent;
  let httpMock: HttpTestingController;

  const userId = '11111111-1111-1111-1111-111111111111';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AdminUsersComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(AdminUsersComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('submits a status change and shows the result', () => {
    fixture.detectChanges();
    component.form.setValue({ userId, status: 'SUSPENDED' });
    component.submit();

    const req = httpMock.expectOne(`/api/v1/admin/users/${userId}/status`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ status: 'SUSPENDED' });
    req.flush({ id: userId, email: 'candidate@jobstack.ma', status: 'SUSPENDED' });

    expect(component.result()).toEqual({ id: userId, email: 'candidate@jobstack.ma', status: 'SUSPENDED' });
  });

  it('does not submit when the form is invalid', () => {
    fixture.detectChanges();
    component.form.setValue({ userId: '', status: 'SUSPENDED' });
    component.submit();

    httpMock.expectNone(`/api/v1/admin/users//status`);
  });

  it('surfaces an error when the update fails', () => {
    fixture.detectChanges();
    component.form.setValue({ userId, status: 'SUSPENDED' });
    component.submit();

    httpMock
      .expectOne(`/api/v1/admin/users/${userId}/status`)
      .flush('not found', { status: 404, statusText: 'Not Found' });

    expect(component.error()).toBe('Could not update that user. Check the user ID and try again.');
  });
});
